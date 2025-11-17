package sandri.sandriweb.domain.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.review.dto.CursorResponseDto;
import sandri.sandriweb.domain.review.dto.ReviewDto;
import sandri.sandriweb.domain.review.dto.CreateReviewRequestDto;
import sandri.sandriweb.domain.review.dto.UpdateReviewRequestDto;
import sandri.sandriweb.domain.review.service.ReviewService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "리뷰", description = "리뷰 관련 API")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/api/places/{placeId}/reviews")
    @Operation(summary = "리뷰 목록 조회 (커서 기반 페이징)", 
               description = "관광지 상세 페이지 및 리뷰 목록 더보기 버튼을 눌러 접근하는 리뷰 더보기 페이지에서 호출합니다." +
                             "관광지의 리뷰 목록을 커서 기반으로 페이징하여 조회합니다. 마지막으로 조회한 리뷰 ID(생략 시 찻 리뷰)를 기준으로 다음 페이지를 가져옵니다." +
                             "마지막으로 조회한 리뷰 ID(그 다음 리뷰부터 조회), 조회할 리뷰 개수, 정렬 기준을 입력받아 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<CursorResponseDto<ReviewDto>>> getReviews(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId,
            @Parameter(description = "마지막으로 조회한 리뷰 ID (첫 조회시 생략)", example = "123")
            @RequestParam(required = false) Long lastReviewId,
            @Parameter(description = "페이지 크기 (한 번에 조회할 개수)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 (latest: 최신순, rating_high: 평점 높은 순, rating_low: 평점 낮은 순)", example = "latest")
            @RequestParam(defaultValue = "latest") String sort) {

        log.info("리뷰 목록 조회 (커서): placeId={}, lastReviewId={}, size={}, sort={}", placeId, lastReviewId, size, sort);

        try {
            // 정렬 옵션 검증
            if (!isValidReviewSort(sort)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("정렬 기준은 'latest', 'rating_high', 'rating_low' 중 하나여야 합니다."));
            }
            
            // 페이지 크기 유효성 검증
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("페이지 크기는 1 이상 100 이하여야 합니다."));
            }

            CursorResponseDto<ReviewDto> response = reviewService.getReviews(placeId, lastReviewId, size, sort);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("리뷰 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("리뷰 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("리뷰 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/api/places/{placeId}/reviews/photos")
    @Operation(summary = "리뷰 사진 목록 조회 (커서 기반 페이징)", 
               description = "관광지 상세 페이지(리뷰 목록 바로 위) 및 리뷰 미디어 페이지에서 호출합니다." +
                             "관광지의 리뷰 사진 목록을 커서 기반으로 페이징하여 조회합니다. 마지막으로 조회한 사진 ID를 기준으로 다음 페이지를 가져옵니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<CursorResponseDto<ReviewDto.PhotoDto>>> getReviewPhotos(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId,
            @Parameter(description = "마지막으로 조회한 사진 ID (첫 조회시 생략)", example = "123")
            @RequestParam(required = false) Long lastPhotoId,
            @Parameter(description = "페이지 크기 (한 번에 조회할 개수)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("리뷰 사진 목록 조회 (커서): placeId={}, lastPhotoId={}, size={}", placeId, lastPhotoId, size);

        try {
            // 페이지 크기 유효성 검증
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("페이지 크기는 1 이상 100 이하여야 합니다."));
            }

            CursorResponseDto<ReviewDto.PhotoDto> response = reviewService.getReviewPhotos(placeId, lastPhotoId, size);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("리뷰 사진 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("리뷰 사진 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("리뷰 사진 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/api/places/{placeId}/reviews")
    @Operation(summary = "리뷰 작성", 
               description = "리뷰 작성 및 수정 페이지에서 리뷰 업로드 시 호출합니다." +
                             "별점, 리뷰 내용, 사진(순서, finalUrl)을 전송하면 리뷰를 작성하고 작성한 리뷰 ID를 반환합니다." +
                             "같은 사용자는 한 장소에 대해 리뷰 하나만 작성가능합니다." +
                             "finalUrl은 프론트에서 getPresignedUrls 호출 시 발급됩니다. 업로드에 성공한 finalUrl을 사용해주세요. " +
                             "작성한 리뷰의 상세 정보가 필요하면 GET /api/me/reviews/{reviewId} API를 호출하세요.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> createReview(
            Authentication authentication,
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            @Valid @RequestBody CreateReviewRequestDto request) {

        String username = authentication.getName();
        log.info("리뷰 작성 요청: username={}, placeId={}", username, placeId);

        try {
            // username으로 User 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            Long reviewId = reviewService.createReview(user, placeId, request);
            return ResponseEntity.ok(ApiResponseDto.success(reviewId));
        } catch (RuntimeException e) {
            log.error("리뷰 작성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("리뷰 작성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("리뷰 작성 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/api/me/reviews/{reviewId}")
    @Operation(summary = "리뷰 상세 조회", 
               description = "리뷰 출력/리뷰 수정 시 호출합니다." +
                             "리뷰 ID, 리뷰 내용, 별점, 작성 일시, 사진 정보 리스트를 반환합니다. (작성자 정보는 null로 반환함.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰 없음")
    })
    public ResponseEntity<ApiResponseDto<ReviewDto>> getReviewById(
            Authentication authentication,
            @Parameter(description = "리뷰 ID", example = "1")
            @PathVariable Long reviewId) {

        String username = authentication.getName();
        log.info("리뷰 상세 조회: username={}, reviewId={}", username, reviewId);

        try {
            // username으로 User 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            ReviewDto response = reviewService.getReviewById(user.getId(), reviewId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("리뷰 상세 조회 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("리뷰 상세 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("리뷰 조회 중 오류가 발생했습니다."));
        }
    }

    @PutMapping("/api/me/reviews/{reviewId}")
    @Operation(summary = "리뷰 수정", 
               description = "마이페이지: 내 리뷰 페이지에서 리뷰 수정 시 호출합니다." +
                             "별점, 리뷰 내용, 사진(순서, finalUrl)을 전송하면 리뷰를 수정하고 수정된 리뷰 ID를 반환합니다. " +
                             "finalUrl은 프론트에서 getPresignedUrls 호출 시 발급됩니다. 업로드에 성공한 finalUrl을 사용해주세요. " +
                             "기존 사진 엔티티는 모두 삭제되고 새로운 사진으로 교체되지만, S3에 저장된 실제 파일은 삭제되지 않습니다. " +
                             "수정된 리뷰의 상세 정보가 필요하면 GET /api/me/reviews/{reviewId} API를 호출하세요.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<Long>> updateReview(
            Authentication authentication,
            @Parameter(description = "리뷰 ID", example = "1")
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequestDto request) {

        String username = authentication.getName();
        log.info("리뷰 수정 요청: username={}, reviewId={}", username, reviewId);

        try {
            // username으로 User 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            Long updatedReviewId = reviewService.updateReview(user.getId(), reviewId, request);
            return ResponseEntity.ok(ApiResponseDto.success(updatedReviewId));
        } catch (RuntimeException e) {
            log.error("리뷰 수정 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("리뷰 수정 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("리뷰 수정 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/api/me/reviews/{reviewId}")
    @Operation(summary = "리뷰 삭제", 
               description = "마이페이지: 내 리뷰 페이지에서 리뷰 삭제 시 호출합니다." +
                             "연결된 사진 엔티티는 삭제되지만, S3에 저장된 실제 파일은 삭제되지 않습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰 없음")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteReview(
            Authentication authentication,
            @Parameter(description = "리뷰 ID", example = "1")
            @PathVariable Long reviewId) {

        String username = authentication.getName();
        log.info("리뷰 삭제 요청: username={}, reviewId={}", username, reviewId);

        try {
            // username으로 User 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            reviewService.deleteReview(user.getId(), reviewId);
            return ResponseEntity.ok(ApiResponseDto.success(null));
        } catch (RuntimeException e) {
            log.error("리뷰 삭제 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("리뷰 삭제 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("리뷰 삭제 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/api/me/reviews")
    @Operation(summary = "내가 작성한 리뷰 목록 조회 (커서 기반 페이징)", 
               description = "현재 로그인한 사용자가 작성한 리뷰 목록을 커서 기반으로 페이징하여 조회합니다. 마지막으로 조회한 리뷰 ID를 기준으로 다음 리뷰를 가져옵니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<CursorResponseDto<ReviewDto>>> getMyReviews(
            Authentication authentication,
            @Parameter(description = "마지막으로 조회한 리뷰 ID (첫 조회시 생략)", example = "123")
            @RequestParam(required = false) Long lastReviewId,
            @Parameter(description = "페이지 크기 (한 번에 조회할 개수)", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        String username = authentication.getName();
        log.info("내가 작성한 리뷰 목록 조회 (커서): username={}, lastReviewId={}, size={}", username, lastReviewId, size);

        try {
            // username으로 User 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // 페이지 크기 유효성 검증
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("페이지 크기는 1 이상 100 이하여야 합니다."));
            }

            CursorResponseDto<ReviewDto> response = reviewService.getMyReviews(user.getId(), lastReviewId, size);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("내가 작성한 리뷰 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("내가 작성한 리뷰 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("리뷰 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }

    private boolean isValidReviewSort(String sort) {
        return sort != null && 
               (sort.equals("latest") || sort.equals("rating_high") || sort.equals("rating_low"));
    }
}

