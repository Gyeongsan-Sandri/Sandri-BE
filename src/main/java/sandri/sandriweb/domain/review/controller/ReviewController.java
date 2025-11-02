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
import org.springframework.web.multipart.MultipartFile;
import sandri.sandriweb.domain.place.dto.PageResponseDto;
import sandri.sandriweb.domain.place.dto.ReviewDto;
import sandri.sandriweb.domain.review.dto.CreateReviewRequestDto;
import sandri.sandriweb.domain.review.dto.UpdateReviewRequestDto;
import sandri.sandriweb.domain.review.dto.UploadFileResponseDto;
import sandri.sandriweb.domain.review.service.ReviewService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.global.service.S3Service;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "리뷰", description = "리뷰 조회 관련 API")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @GetMapping("/places/{placeId}")
    @Operation(summary = "리뷰 목록 조회 (페이징)", 
               description = "관광지의 리뷰 목록을 페이징하여 조회합니다. 정렬 옵션과 페이지 크기를 지정할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<ReviewDto>>> getReviews(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (한 번에 조회할 개수)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 (latest: 최신순, rating_high: 평점 높은 순, rating_low: 평점 낮은 순)", example = "latest")
            @RequestParam(defaultValue = "latest") String sort) {

        log.info("리뷰 목록 조회: placeId={}, page={}, size={}, sort={}", placeId, page, size, sort);

        try {
            // 정렬 옵션 검증
            if (!isValidReviewSort(sort)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("정렬 기준은 'latest', 'rating_high', 'rating_low' 중 하나여야 합니다."));
            }
            
            // 페이지 번호 유효성 검증
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("페이지 번호는 0 이상이어야 합니다."));
            }
            
            // 페이지 크기 유효성 검증
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("페이지 크기는 1 이상 100 이하여야 합니다."));
            }

            PageResponseDto<ReviewDto> response = reviewService.getReviews(placeId, page, size, sort);
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

    @GetMapping("/places/{placeId}/photos")
    @Operation(summary = "리뷰 사진 목록 조회", 
               description = "관광지의 리뷰 사진 목록을 조회합니다. 요청한 개수만큼 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<List<String>>> getReviewPhotos(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId,
            @Parameter(description = "조회할 사진 개수", example = "20")
            @RequestParam(defaultValue = "20") int count) {

        log.info("리뷰 사진 목록 조회: placeId={}, count={}", placeId, count);

        try {
            // 개수 유효성 검증
            if (count <= 0 || count > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("조회할 개수는 1 이상 100 이하여야 합니다."));
            }

            List<String> response = reviewService.getReviewPhotos(placeId, count);
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

    @PostMapping("/api/me/reviews/files")
    @Operation(summary = "리뷰 사진/영상 업로드", 
               description = "리뷰에 첨부할 사진/영상을 AWS S3에 업로드합니다. 업로드된 URL을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "파일 업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<UploadFileResponseDto>> uploadFiles(
            Authentication authentication,
            @Parameter(description = "업로드할 파일들 (사진/영상)", required = true)
            @RequestParam("files") List<MultipartFile> files) {

        String username = authentication.getName();
        log.info("파일 업로드 요청: username={}, fileCount={}", username, files.size());

        try {
            // 파일 개수 검증
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("업로드할 파일이 없습니다."));
            }

            // 파일 개수 제한 (최대 10개)
            if (files.size() > 10) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("최대 10개까지 업로드 가능합니다."));
            }

            // AWS S3에 파일 업로드
            List<String> fileUrls = s3Service.uploadFiles(files);

            UploadFileResponseDto response = UploadFileResponseDto.builder()
                    .fileUrls(fileUrls)
                    .build();

            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (UnsupportedOperationException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("AWS S3 업로드 기능이 아직 구현되지 않았습니다."));
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("파일 업로드 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/api/me/reviews/places/{placeId}")
    @Operation(summary = "리뷰 작성", 
               description = "현재 로그인한 사용자가 특정 장소에 대한 리뷰를 작성합니다. 사진/영상은 AWS S3에 업로드된 URL을 photoUrls에 포함하여 전송합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<ReviewDto>> createReview(
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

            ReviewDto response = reviewService.createReview(user.getId(), placeId, request);
            return ResponseEntity.ok(ApiResponseDto.success(response));
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
               description = "현재 로그인한 사용자가 작성한 특정 리뷰의 상세 정보를 조회합니다. 리뷰 수정 페이지에서 사용됩니다.")
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
               description = "현재 로그인한 사용자가 작성한 리뷰를 수정합니다. 사진/영상은 AWS S3에 업로드된 URL을 photoUrls에 포함하여 전송하며, 기존 사진은 삭제됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<ReviewDto>> updateReview(
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

            ReviewDto response = reviewService.updateReview(user.getId(), reviewId, request);
            return ResponseEntity.ok(ApiResponseDto.success(response));
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
               description = "현재 로그인한 사용자가 작성한 리뷰를 삭제합니다. 연결된 사진도 함께 삭제됩니다.")
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
    @Operation(summary = "내가 작성한 리뷰 목록 조회", 
               description = "현재 로그인한 사용자가 작성한 리뷰 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<ReviewDto>>> getMyReviews(
            Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (한 번에 조회할 개수)", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        String username = authentication.getName();
        log.info("내가 작성한 리뷰 목록 조회: username={}, page={}, size={}", username, page, size);

        try {
            // username으로 User 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // 페이지 번호 유효성 검증
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("페이지 번호는 0 이상이어야 합니다."));
            }

            // 페이지 크기 유효성 검증
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("페이지 크기는 1 이상 100 이하여야 합니다."));
            }

            PageResponseDto<ReviewDto> response = reviewService.getMyReviews(user.getId(), page, size);
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

