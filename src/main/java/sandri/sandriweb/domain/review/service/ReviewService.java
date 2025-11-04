package sandri.sandriweb.domain.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.review.dto.PageResponseDto;
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
     * @param userId 사용자 ID
     * @param placeId 장소 ID
     * @param request 리뷰 작성 요청 DTO
     * @return 작성된 리뷰 DTO
     */
    @Transactional
    public ReviewDto createReview(Long userId, Long placeId, CreateReviewRequestDto request) {
        // Place 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

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
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            photos = request.getPhotoUrls().stream()
                    .map(photoUrl -> PlaceReviewPhoto.builder()
                            .placeReview(savedReview)
                            .place(place)
                            .photoUrl(photoUrl)
                            .build())
                    .collect(Collectors.toList());

            photos = placeReviewPhotoRepository.saveAll(photos);
        }

        log.info("리뷰 작성 완료: reviewId={}, userId={}, placeId={}", savedReview.getId(), userId, placeId);

        // 저장된 리뷰에 photos 설정 (같은 트랜잭션 내에서 lazy loading이 작동하므로 자동으로 로드됨)
        return ReviewDto.from(savedReview);
    }

    /**
     * 리뷰 수정
     * @param userId 사용자 ID
     * @param reviewId 리뷰 ID
     * @param request 리뷰 수정 요청 DTO
     * @return 수정된 리뷰 DTO
     */
    @Transactional
    public ReviewDto updateReview(Long userId, Long reviewId, UpdateReviewRequestDto request) {
        // 리뷰 조회 및 소유자 확인
        PlaceReview review = placeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        // 리뷰 내용 수정
        review.update(request.getRating(), request.getContent());
        PlaceReview updatedReview = placeReviewRepository.save(review);

        // 기존 사진 삭제
        deleteReviewPhotos(reviewId);

        // 새로운 사진 저장
        List<PlaceReviewPhoto> newPhotos = new ArrayList<>();
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            newPhotos = request.getPhotoUrls().stream()
                    .map(photoUrl -> PlaceReviewPhoto.builder()
                            .placeReview(updatedReview)
                            .place(updatedReview.getPlace())
                            .photoUrl(photoUrl)
                            .build())
                    .collect(Collectors.toList());

            placeReviewPhotoRepository.saveAll(newPhotos);
        }

        log.info("리뷰 수정 완료: reviewId={}, userId={}", reviewId, userId);

        // 수정된 리뷰 조회 (사진 포함)
        PlaceReview reviewWithPhotos = placeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("수정된 리뷰를 찾을 수 없습니다."));

        return ReviewDto.from(reviewWithPhotos);
    }

    /**
     * 리뷰 삭제
     * @param userId 사용자 ID
     * @param reviewId 리뷰 ID
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        // 리뷰 조회 및 소유자 확인
        PlaceReview review = placeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        // 리뷰에 연결된 사진들 삭제 (S3에서도 삭제)
        deleteReviewPhotos(reviewId);

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
        
        // 커서 기반 페이징 처리
        return buildCursorResponse(allReviews, size, ReviewDto::from, PlaceReview::getId);
    }

    /**
     * 리뷰 사진 목록 조회 (커서 기반 페이징)
     * @param placeId 관광지 ID
     * @param lastPhotoId 마지막으로 조회한 사진 ID (첫 조회시 null)
     * @param size 페이지 크기
     * @return 커서 기반 페이징된 리뷰 사진 URL 리스트
     */
    @Transactional(readOnly = true)
    public CursorResponseDto<String> getReviewPhotos(Long placeId, Long lastPhotoId, int size) {
        // size + 1개를 가져와서 다음 페이지 존재 여부 확인
        Pageable pageable = PageRequest.of(0, size + 1);
        
        List<PlaceReviewPhoto> allPhotos = placeReviewPhotoRepository.findByPlaceIdWithCursor(placeId, lastPhotoId, pageable);
        
        // 커서 기반 페이징 처리
        return buildCursorResponse(allPhotos, size, PlaceReviewPhoto::getPhotoUrl, PlaceReviewPhoto::getId);
    }

    /**
     * 리뷰 목록 조회 (정렬 옵션 포함, 상세 정보용)
     * @param placeId 관광지 ID
     * @param count 조회할 개수
     * @param sort 정렬 기준
     * @return 리뷰 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByPlaceId(Long placeId, int count, String sort) {
        // 정렬 옵션에 따라 다른 메서드 호출
        List<PlaceReview> reviews;
        switch (sort) {
            case "rating_high":
                reviews = placeReviewRepository.findReviewsByPlaceIdOrderByRatingDesc(placeId);
                break;
            case "rating_low":
                reviews = placeReviewRepository.findReviewsByPlaceIdOrderByRatingAsc(placeId);
                break;
            case "latest":
            default:
                reviews = placeReviewRepository.findReviewsByPlaceIdOrderByLatest(placeId);
                break;
        }
        
        return reviews.stream()
                .limit(count)
                .map(ReviewDto::from)
                .collect(Collectors.toList());
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
     * 여러 장소의 평균 평점을 한 번에 조회 (배치 조회)
     * @param placeIds 장소 ID 목록
     * @return Place ID를 키로, 평균 평점을 값으로 하는 Map
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> getAverageRatingsByPlaceIds(List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return new HashMap<>();
        }
        
        List<Object[]> results = placeReviewRepository.findAverageRatingsByPlaceIds(placeIds);
        
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> result[1] != null ? ((Double) result[1]) : 0.0
                ));
    }

    /**
     * 리뷰 상세 조회
     * @param userId 사용자 ID
     * @param reviewId 리뷰 ID
     * @return 리뷰 DTO
     */
    @Transactional(readOnly = true)
    public ReviewDto getReviewById(Long userId, Long reviewId) {
        // 리뷰 조회 및 소유자 확인
        PlaceReview review = placeReviewRepository.findById(reviewId)
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

        // 커서 기반 페이징 처리
        return buildCursorResponse(allReviews, size, ReviewDto::from, PlaceReview::getId);
    }

    /**
     * 커서 기반 페이징 응답 생성 (공통 헬퍼 메서드)
     * @param allItems 전체 아이템 리스트 (size + 1개)
     * @param size 요청한 페이지 크기
     * @param mapper 아이템을 DTO로 변환하는 함수
     * @param idExtractor 아이템의 ID를 추출하는 함수 (커서용)
     * @return 커서 기반 페이징 응답
     */
    private <T, D> CursorResponseDto<D> buildCursorResponse(
            List<T> allItems,
            int size,
            Function<T, D> mapper,
            Function<T, Long> idExtractor) {
        
        // size + 1개를 확인하여 다음 페이지 존재 여부 판단
        boolean hasNext = allItems.size() > size;
        List<T> items = hasNext 
                ? allItems.subList(0, size) 
                : allItems;

        // DTO 변환
        List<D> content = items.stream()
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
                .build();
    }

    /**
     * 리뷰에 연결된 사진 삭제 (S3 및 DB에서 삭제)
     * @param reviewId 리뷰 ID
     */
    private void deleteReviewPhotos(Long reviewId) {
        List<PlaceReviewPhoto> photos = placeReviewPhotoRepository.findByPlaceReviewId(reviewId);
        if (!photos.isEmpty()) {
            // S3에서 기존 사진 삭제
            photos.forEach(photo -> {
                try {
                    s3Service.deleteFile(photo.getPhotoUrl());
                } catch (Exception e) {
                    log.warn("S3 파일 삭제 실패 (무시): {}", photo.getPhotoUrl(), e);
                }
            });
            placeReviewPhotoRepository.deleteAll(photos);
        }
    }
}

