package sandri.sandriweb.domain.favorite.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.favorite.dto.FavoriteListResponseDto;
import sandri.sandriweb.domain.favorite.enums.FavoriteType;
import sandri.sandriweb.domain.favorite.service.FavoriteService;
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.route.service.RouteService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final PlaceService placeService;
    private final RouteService routeService;
    private final MagazineService magazineService;

    @GetMapping
    @Operation(summary = "관심 목록 조회", description = "사용자가 찜한 관광지, 루트, 매거진 목록을 모두 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FavoriteListResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<FavoriteListResponseDto>> getFavorites(
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponseDto.error("로그인이 필요합니다."));
        }

        FavoriteListResponseDto favorites = favoriteService.getFavoriteList(user.getId());
        return ResponseEntity.ok(ApiResponseDto.success(favorites));
    }

    @PostMapping("/{type}/{targetId}")
    @Operation(summary = "관심 등록/해제",
            description = "관심 대상을 토글합니다. type에는 PLACE, ROUTE, MAGAZINE 중 하나를 전달합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토글 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = "{\n  \"success\": true,\n  \"message\": \"관심에 추가되었습니다.\",\n  \"data\": true\n}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "대상 없음")
    })
    public ResponseEntity<ApiResponseDto<Boolean>> toggleFavorite(
            @PathVariable FavoriteType type,
            @PathVariable Long targetId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponseDto.error("로그인이 필요합니다."));
        }

        try {
            boolean isLiked;
            switch (type) {
                case PLACE -> isLiked = placeService.toggleLike(targetId, user.getId());
                case ROUTE -> isLiked = routeService.toggleLike(targetId, user.getId());
                case MAGAZINE -> isLiked = magazineService.toggleLike(targetId, user.getId());
                default -> throw new IllegalArgumentException("지원하지 않는 관심 타입입니다.");
            }

            String message = isLiked ? "관심에 추가되었습니다." : "관심이 취소되었습니다.";
            return ResponseEntity.ok(ApiResponseDto.success(message, isLiked));
        } catch (RuntimeException e) {
            log.error("관심 토글 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("관심 토글 중 예상치 못한 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("관심 처리 중 오류가 발생했습니다."));
        }
    }
}

