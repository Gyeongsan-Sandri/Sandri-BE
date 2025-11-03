package sandri.sandriweb.domain.place.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.place.dto.CategoryPlaceDto;
import sandri.sandriweb.domain.place.dto.NearbyPlaceDto;
import sandri.sandriweb.domain.place.dto.PlaceDetailResponseDto;
import sandri.sandriweb.domain.place.enums.PlaceCategory;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관광지", description = "관광지 정보 관련 API")
public class PlaceController {

    private final PlaceService placeService;
    private final UserRepository userRepository;

    @GetMapping("/{placeId}")
    @Operation(summary = "관광지 상세 정보 조회", 
               description = "관광지의 기본 정보를 조회합니다. 이름, 주소, 평점, 카테고리, 공식 사진, 근처 가볼만한 곳을 반환합니다. " +
                           "리뷰 정보는 /api/places/{placeId}/reviews API를 별도로 호출하세요.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<PlaceDetailResponseDto>> getPlaceDetail(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId) {

        log.info("관광지 상세 정보 조회: placeId={}", placeId);

        try {
            PlaceDetailResponseDto response = placeService.getPlaceDetail(placeId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("관광지 상세 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("관광지 상세 정보 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("관광지 정보를 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{placeId}/nearby")
    @Operation(summary = "근처 가볼만한 곳 조회", 
               description = "특정 관광지 근처의 카테고리별 추천 장소 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<List<NearbyPlaceDto>>> getNearbyPlaces(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId,
            @Parameter(description = "카테고리 (관광지/맛집/카페)", example = "맛집")
            @RequestParam String category,
            @Parameter(description = "조회할 개수", example = "3")
            @RequestParam(defaultValue = "3") int count) {

        log.info("근처 장소 조회: placeId={}, category={}, count={}", placeId, category, count);

        try {
            // 카테고리 검증
            if (!isValidCategory(category)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("카테고리는 '관광지', '맛집', '카페' 중 하나여야 합니다."));
            }

            List<NearbyPlaceDto> response = placeService.getNearbyPlacesByPlaceId(placeId, category, count);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("근처 장소 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("근처 장소 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("근처 장소를 조회하는 중 오류가 발생했습니다."));
        }
    }

    private boolean isValidCategory(String category) {
        if (category == null) {
            return false;
        }
        try {
            PlaceCategory.valueOf(category);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @GetMapping("/category")
    @Operation(summary = "카테고리별 장소 조회",
               description = "카테고리별로 좋아요가 많은 순으로 장소 목록을 조회합니다. 로그인한 경우 사용자가 좋아요한 장소 여부도 함께 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<CategoryPlaceDto>>> getPlacesByCategory(
            @Parameter(description = "카테고리 ('자연/힐링', '역사/전통', '문화/체험', '식도락' 중 하나)", example = "자연/힐링")
            @RequestParam String category,
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int count,
            Authentication authentication) {

        log.info("카테고리별 장소 조회: category={}, count={}", category, count);

        try {
            // 카테고리 검증
            if (!isValidCategoryDisplayName(category)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("카테고리는 '자연/힐링', '역사/전통', '문화/체험', '식도락' 중 하나여야 합니다."));
            }

            // 사용자 ID 조회 (로그인한 경우)
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    User user = userRepository.findByUsername(authentication.getName())
                            .orElse(null);
                    if (user != null) {
                        userId = user.getId();
                    }
                } catch (Exception e) {
                    log.debug("사용자 정보 조회 실패 (무시): {}", e.getMessage());
                }
            }

            List<CategoryPlaceDto> response = placeService.getPlacesByCategory(category, count, userId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("카테고리별 장소 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("카테고리별 장소 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("카테고리별 장소를 조회하는 중 오류가 발생했습니다."));
        }
    }

    private boolean isValidCategoryDisplayName(String categoryDisplayName) {
        if (categoryDisplayName == null) {
            return false;
        }
        return categoryDisplayName.equals("자연/힐링") ||
               categoryDisplayName.equals("역사/전통") ||
               categoryDisplayName.equals("문화/체험") ||
               categoryDisplayName.equals("식도락");
    }

    @PostMapping("/{placeId}/like")
    @Operation(summary = "장소 좋아요 토글",
               description = "장소에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 좋아요하지 않은 경우 추가됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<Boolean>> toggleLike(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            Authentication authentication) {

        log.info("장소 좋아요 토글: placeId={}", placeId);

        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(ApiResponseDto.error("로그인이 필요합니다."));
            }

            // 사용자 ID 조회
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 좋아요 토글
            boolean isLiked = placeService.toggleLike(placeId, user.getId());
            
            String message = isLiked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.";
            return ResponseEntity.ok(ApiResponseDto.success(message, isLiked));

        } catch (RuntimeException e) {
            log.error("장소 좋아요 토글 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장소 좋아요 토글 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("좋아요 처리 중 오류가 발생했습니다."));
        }
    }


}

