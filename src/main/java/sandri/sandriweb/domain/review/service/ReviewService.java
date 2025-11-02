package sandri.sandriweb.domain.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.place.dto.PageResponseDto;
import sandri.sandriweb.domain.place.dto.ReviewDto;
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
import java.util.List;
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
        List<PlaceReviewPhoto> existingPhotos = placeReviewPhotoRepository.findByPlaceReviewId(reviewId);
        if (!existingPhotos.isEmpty()) {
            // S3에서 기존 사진 삭제
            existingPhotos.forEach(photo -> {
                try {
                    s3Service.deleteFile(photo.getPhotoUrl());
                } catch (Exception e) {
                    log.warn("S3 파일 삭제 실패 (무시): {}", photo.getPhotoUrl(), e);
                }
            });
            placeReviewPhotoRepository.deleteAll(existingPhotos);
        }

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
        List<PlaceReviewPhoto> photos = placeReviewPhotoRepository.findByPlaceReviewId(reviewId);
        if (!photos.isEmpty()) {
            photos.forEach(photo -> {
                try {
                    s3Service.deleteFile(photo.getPhotoUrl());
                } catch (Exception e) {
                    log.warn("S3 파일 삭제 실패 (무시): {}", photo.getPhotoUrl(), e);
                }
            });
            placeReviewPhotoRepository.deleteAll(photos);
        }

        // 리뷰 삭제
        placeReviewRepository.delete(review);

        log.info("리뷰 삭제 완료: reviewId={}, userId={}", reviewId, userId);
    }

    /**
     * 리뷰 목록 조회 (페이징)
     * @param placeId 관광지 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 기준 (latest: 최신순, rating_high: 평점 높은 순, rating_low: 평점 낮은 순)
     * @return 페이징된 리뷰 목록
     */
    @Transactional(readOnly = true)
    public PageResponseDto<ReviewDto> getReviews(Long placeId, int page, int size, String sort) {
        // Pageable 생성 (0부터 시작)
        Pageable pageable = PageRequest.of(page, size);
        
        // 정렬 옵션에 따라 다른 메서드 호출
        Page<PlaceReview> reviewPage;
        switch (sort) {
            case "rating_high":
                reviewPage = placeReviewRepository.findReviewsByPlaceIdOrderByRatingDescWithPaging(placeId, pageable);
                break;
            case "rating_low":
                reviewPage = placeReviewRepository.findReviewsByPlaceIdOrderByRatingAscWithPaging(placeId, pageable);
                break;
            case "latest":
            default:
                reviewPage = placeReviewRepository.findReviewsByPlaceIdOrderByLatestWithPaging(placeId, pageable);
                break;
        }
        
        // DTO 변환
        List<ReviewDto> reviewDtos = reviewPage.getContent().stream()
                .map(ReviewDto::from)
                .collect(Collectors.toList());
        
        // PageResponseDto 생성
        return PageResponseDto.<ReviewDto>builder()
                .content(reviewDtos)
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                .build();
    }

    /**
     * 리뷰 사진 목록 조회
     * @param placeId 관광지 ID
     * @param count 조회할 사진 개수
     * @return 리뷰 사진 URL 리스트
     */
    @Transactional(readOnly = true)
    public List<String> getReviewPhotos(Long placeId, int count) {
        List<PlaceReviewPhoto> reviewPhotos = placeReviewPhotoRepository.findByPlaceId(placeId);
        return reviewPhotos.stream()
                .limit(count)
                .map(PlaceReviewPhoto::getPhotoUrl)
                .collect(Collectors.toList());
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
     * 내가 작성한 리뷰 목록 조회 (페이징)
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 리뷰 목록
     */
    @Transactional(readOnly = true)
    public PageResponseDto<ReviewDto> getMyReviews(Long userId, int page, int size) {
        // Pageable 생성 (0부터 시작)
        Pageable pageable = PageRequest.of(page, size);

        // 최신순으로 내가 작성한 리뷰 조회
        Page<PlaceReview> reviewPage = placeReviewRepository.findReviewsByUserIdOrderByLatestWithPaging(userId, pageable);

        // DTO 변환
        List<ReviewDto> reviewDtos = reviewPage.getContent().stream()
                .map(ReviewDto::from)
                .collect(Collectors.toList());

        // PageResponseDto 생성
        return PageResponseDto.<ReviewDto>builder()
                .content(reviewDtos)
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                .build();
    }
}

