package sandri.sandriweb.domain.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.review.dto.ReviewListDto;
import sandri.sandriweb.domain.review.dto.CursorResponseDto;
import sandri.sandriweb.domain.review.dto.ReviewDto;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.review.dto.CreateReviewRequestDto;
import sandri.sandriweb.domain.review.dto.UpdateReviewRequestDto;
import sandri.sandriweb.domain.review.entity.PlaceReview;
import sandri.sandriweb.domain.review.entity.PlaceReviewPhoto;
import sandri.sandriweb.domain.review.repository.PlaceReviewPhotoRepository;
import sandri.sandriweb.domain.review.repository.PlaceReviewRepository;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.global.service.S3Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewPhotoRepository placeReviewPhotoRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 리뷰 작성
     * @param user 사용자 엔티티 (컨트롤러에서 조회하여 전달)
     * @param placeId 장소 ID
     * @param request 리뷰 작성 요청 DTO
     * @return 작성된 리뷰 ID
     */
    @Transactional
    public Long createReview(User user, Long placeId, CreateReviewRequestDto request) {
        // Place 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        // 중복 리뷰 체크: 한 사용자는 한 장소에 대해 리뷰 하나만 작성 가능
        // 호출될 일 X
        if (placeReviewRepository.existsByUserIdAndPlaceId(user.getId(), placeId)) {
            throw new RuntimeException("이미 해당 장소에 리뷰를 작성하셨습니다. 리뷰 수정을 이용해주세요.");
        }

        // TODO: 방문 기록 테이블에서 visitDate 가져오기 (나중에 방문 기록 테이블 연동 시)
        // LocalDate visitDate = visitRecordRepository.findByUserIdAndPlaceId(userId, placeId)
        //         .orElseThrow(() -> new RuntimeException("방문 기록을 찾을 수 없습니다."))
        //         .getVisitDate();

        // PlaceReview 생성 (visitDate는 방문 기록 테이블 연동 후 설정)
        PlaceReview review = PlaceReview.builder()
                .place(place)
                .user(user)
                .rating(request.getRating())
                .content(request.getContent())
                .photos(new ArrayList<>())
                .build();

        PlaceReview savedReview = placeReviewRepository.save(review);

        // 사진이 있는 경우 PlaceReviewPhoto 저장
        List<PlaceReviewPhoto> photos = new ArrayList<>();
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            photos = request.getPhotos().stream()
                    .map(photoInfo -> PlaceReviewPhoto.builder()
                            .placeReview(savedReview)
                            .place(place)
                            .photoUrl(photoInfo.getPhotoUrl())
                            .order(photoInfo.getOrder())
                            .build())
                    .collect(Collectors.toList());

            photos = placeReviewPhotoRepository.saveAll(photos);
        }

        log.info("리뷰 작성 완료: reviewId={}, userId={}, placeId={}", savedReview.getId(), user.getId(), placeId);

        // 리뷰 ID만 반환
        return savedReview.getId();
    }

    /**
     * 리뷰 수정
     * @param userId 사용자 ID
     * @param reviewId 리뷰 ID
     * @param request 리뷰 수정 요청 DTO
     * @return 수정된 리뷰 ID
     */
    @Transactional
    public Long updateReview(Long userId, Long reviewId, UpdateReviewRequestDto request) {
        // 리뷰 조회 및 소유자 확인 (사용자 및 장소 정보 포함하여 N+1 방지)
        PlaceReview review = placeReviewRepository.findByIdWithUserAndPlace(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        // 리뷰 내용 수정
        review.update(request.getRating(), request.getContent());
        PlaceReview updatedReview = placeReviewRepository.save(review);

        // 사진 업데이트 (요청에 photos가 포함된 경우)
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            // 기존 enabled된 사진 조회
            List<PlaceReviewPhoto> existingPhotos = placeReviewPhotoRepository.findEnabledByReviewId(reviewId);
            
            // 기존 사진을 order로 매핑
            Map<Integer, PlaceReviewPhoto> existingPhotosByOrder = new HashMap<>();
            if (!existingPhotos.isEmpty()) {
                existingPhotosByOrder = existingPhotos.stream()
                        .collect(Collectors.toMap(
                                PlaceReviewPhoto::getOrder,
                                photo -> photo,
                                (existing, replacement) -> existing // 중복 시 기존 것 유지
                        ));
            }

            // 요청된 사진 정보로 업데이트 또는 생성
            List<PlaceReviewPhoto> photosToSave = new ArrayList<>();
            for (CreateReviewRequestDto.PhotoInfo photoInfo : request.getPhotos()) {
                Integer order = photoInfo.getOrder();
                String photoUrl = photoInfo.getPhotoUrl();
                
                // photoUrl이 빈 문자열이면 disable 처리
                if (photoUrl != null && photoUrl.trim().isEmpty()) {
                    PlaceReviewPhoto existingPhoto = existingPhotosByOrder.get(order);
                    if (existingPhoto != null) {
                        existingPhoto.disable();
                        photosToSave.add(existingPhoto);
                    }
                } else if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                    // photoUrl이 있으면 업데이트 또는 생성
                    PlaceReviewPhoto existingPhoto = existingPhotosByOrder.get(order);
                    if (existingPhoto != null) {
                        // 기존 사진이 있으면 URL만 업데이트하고 enable
                        existingPhoto.updatePhotoUrl(photoUrl);
                        existingPhoto.enable();
                        photosToSave.add(existingPhoto);
                    } else {
                        // 기존 사진이 없으면 새로 생성
                        PlaceReviewPhoto newPhoto = PlaceReviewPhoto.builder()
                                .placeReview(updatedReview)
                                .place(updatedReview.getPlace())
                                .photoUrl(photoUrl)
                                .order(order)
                                .enabled(true)
                                .build();
                        photosToSave.add(newPhoto);
                    }
                }
            }
            
            // 변경사항 저장
            if (!photosToSave.isEmpty()) {
                placeReviewPhotoRepository.saveAll(photosToSave);
            }

            log.info("리뷰 사진 업데이트 완료: reviewId={}, processedCount={}", 
                     reviewId, request.getPhotos().size());
        }

        log.info("리뷰 수정 완료: reviewId={}, userId={}", reviewId, userId);

        // 리뷰 ID만 반환
        return reviewId;
    }

    /**
     * 리뷰 삭제
     * @param userId 사용자 ID
     * @param reviewId 리뷰 ID
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        // 리뷰 조회 및 소유자 확인 (사용자 정보 포함하여 N+1 방지)
        PlaceReview review = placeReviewRepository.findByIdWithUserAndPlace(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        // 권한 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        // 리뷰에 연결된 사진들 삭제 (엔티티만 삭제, S3에서는 삭제하지 않음)
        List<PlaceReviewPhoto> photos = placeReviewPhotoRepository.findByPlaceReviewId(reviewId);
        if (!photos.isEmpty()) {
            placeReviewPhotoRepository.deleteAll(photos);
        }

        // 리뷰 삭제
        placeReviewRepository.delete(review);

        log.info("리뷰 삭제 완료: reviewId={}, userId={}", reviewId, userId);
    }

    /**
     * 리뷰 목록 조회 (커서 기반 페이징)
     * @param placeId 관광지 ID
     * @param lastReviewId 마지막으로 조회한 리뷰 ID (첫 조회시 null)
     * @param size 페이지 크기
     * @param sort 정렬 기준 (latest: 최신순, rating_high: 평점 높은 순, rating_low: 평점 낮은 순)
     * @return 커서 기반 페이징된 리뷰 목록
     */
    @Transactional(readOnly = true)
    public CursorResponseDto<ReviewDto> getReviews(Long placeId, Long lastReviewId, int size, String sort) {
        // size + 1개를 가져와서 다음 페이지 존재 여부 확인
        Pageable pageable = PageRequest.of(0, size + 1);
        
        // 정렬 옵션에 따라 다른 메서드 호출
        List<PlaceReview> allReviews;
        switch (sort) {
            case "rating_high":
                allReviews = placeReviewRepository.findReviewsByPlaceIdOrderByRatingDescWithCursor(placeId, lastReviewId, pageable);
                break;
            case "rating_low":
                allReviews = placeReviewRepository.findReviewsByPlaceIdOrderByRatingAscWithCursor(placeId, lastReviewId, pageable);
                break;
            case "latest":
            default:
                allReviews = placeReviewRepository.findReviewsByPlaceIdOrderByLatestWithCursor(placeId, lastReviewId, pageable);
                break;
        }
        
        // 총 리뷰 개수 조회
        Long totalCount = placeReviewRepository.countByPlaceId(placeId);
        
        // 커서 기반 페이징 처리
        return buildCursorResponse(allReviews, size, ReviewDto::from, PlaceReview::getId, totalCount);
    }

    /**
     * 리뷰 사진 목록 조회 (커서 기반 페이징)
     * @param placeId 관광지 ID
     * @param lastPhotoId 마지막으로 조회한 사진 ID (첫 조회시 null)
     * @param size 페이지 크기
     * @return 커서 기반 페이징된 리뷰 사진 정보 리스트 (order 포함)
     */
    @Transactional(readOnly = true)
    public CursorResponseDto<ReviewDto.PhotoDto> getReviewPhotos(Long placeId, Long lastPhotoId, int size) {
        // size + 1개를 가져와서 다음 페이지 존재 여부 확인
        Pageable pageable = PageRequest.of(0, size + 1);
        
        List<PlaceReviewPhoto> allPhotos = placeReviewPhotoRepository.findByPlaceIdWithCursor(placeId, lastPhotoId, pageable);
        
        // 총 리뷰 사진 개수 조회
        Long totalCount = placeReviewPhotoRepository.countByPlaceId(placeId);
        
        // size + 1개를 확인하여 다음 페이지 존재 여부 판단
        boolean hasNext = allPhotos.size() > size;
        List<PlaceReviewPhoto> photos = hasNext 
                ? allPhotos.subList(0, size) 
                : allPhotos;
        
        // order를 0부터 연속적으로 재정렬 (각 페이지 내에서)
        List<ReviewDto.PhotoDto> content = new ArrayList<>();
        int index = 0;
        for (PlaceReviewPhoto photo : photos) {
            content.add(ReviewDto.PhotoDto.builder()
                    .photoUrl(photo.getPhotoUrl())
                    .order(index++) // 0부터 연속적으로 재정렬
                    .build());
        }
        
        // 다음 커서 설정
        Long nextCursor = (hasNext && !photos.isEmpty()) 
                ? photos.get(photos.size() - 1).getId() 
                : null;
        
        return CursorResponseDto.<ReviewDto.PhotoDto>builder()
                .content(content)
                .size(size)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 평균 평점 조회
     * @param placeId 관광지 ID
     * @return 평균 평점
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(Long placeId) {
        Double averageRating = placeReviewRepository.findAverageRatingByPlaceId(placeId);
        return averageRating != null ? averageRating : 0.0;
    }

    /**
     * 리뷰 상세 조회
     * @param userId 사용자 ID
     * @param reviewId 리뷰 ID
     * @return 리뷰 DTO
     */
    @Transactional(readOnly = true)
    public ReviewDto getReviewById(Long userId, Long reviewId) {
        // 리뷰 조회 및 소유자 확인 (사진과 사용자 정보 포함)
        PlaceReview review = placeReviewRepository.findByIdWithPhotos(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        // 본인이 작성한 리뷰인지 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 조회할 권한이 없습니다.");
        }

        return ReviewDto.from(review);
    }

    /**
     * 내가 작성한 리뷰 목록 조회 (커서 기반 페이징)
     * @param userId 사용자 ID
     * @param lastReviewId 마지막으로 조회한 리뷰 ID (첫 조회시 null)
     * @param size 페이지 크기
     * @return 커서 기반 페이징된 리뷰 목록
     */
    @Transactional(readOnly = true)
    public CursorResponseDto<ReviewDto> getMyReviews(Long userId, Long lastReviewId, int size) {
        // size + 1개를 가져와서 다음 페이지 존재 여부 확인
        Pageable pageable = PageRequest.of(0, size + 1);

        // 최신순으로 내가 작성한 리뷰 조회 (커서 기반)
        List<PlaceReview> allReviews = placeReviewRepository.findReviewsByUserIdOrderByLatestWithCursor(userId, lastReviewId, pageable);

        // 커서 기반 페이징 처리 (내가 작성한 리뷰이므로 사용자 정보 제외)
        return buildCursorResponse(allReviews, size, review -> ReviewDto.from(review, false), PlaceReview::getId, null);
    }

    /**
     * 관리자용 전체 리뷰 목록 조회 (reviewId와 content만 반환)
     * @return 전체 리뷰 목록 (reviewId, content)
     */
    @Transactional(readOnly = true)
    public List<ReviewListDto> getAllReviews() {
        List<PlaceReview> reviews = placeReviewRepository.findAll();
        
        return reviews.stream()
                .filter(PlaceReview::isEnabled) // enabled된 것만
                .map(review -> ReviewListDto.builder()
                        .reviewId(review.getId())
                        .content(review.getContent())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 커서 기반 페이징 응답 생성 (공통 헬퍼 메서드)
     * @param allItems 전체 아이템 리스트 (size + 1개)
     * @param size 요청한 페이지 크기
     * @param mapper 아이템을 DTO로 변환하는 함수
     * @param idExtractor 아이템의 ID를 추출하는 함수 (커서용)
     * @param totalCount 전체 개수 (선택사항, 리뷰 목록 조회 시에만 사용)
     * @return 커서 기반 페이징 응답
     */
    private <T, D> CursorResponseDto<D> buildCursorResponse(
            List<T> allItems,
            int size,
            Function<T, D> mapper,
            Function<T, Long> idExtractor,
            Long totalCount) {
        
        // size + 1개를 확인하여 다음 페이지 존재 여부 판단
        boolean hasNext = allItems.size() > size;
        List<T> items = hasNext 
                ? allItems.subList(0, size) 
                : allItems;

        // DTO 변환
        List<D> content = (List<D>) items.stream()
                .map(mapper)
                .collect(Collectors.toList());

        // 마지막 아이템 ID 추출 (다음 커서)
        Long nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            nextCursor = idExtractor.apply(items.get(items.size() - 1));
        }

        // CursorResponseDto 생성
        return CursorResponseDto.<D>builder()
                .content(content)
                .size(size)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .build();
    }

}

