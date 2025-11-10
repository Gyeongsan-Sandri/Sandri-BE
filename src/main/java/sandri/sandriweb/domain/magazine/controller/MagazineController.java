package sandri.sandriweb.domain.magazine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.magazine.dto.MagazineDetailResponseDto;
import sandri.sandriweb.domain.magazine.dto.MagazineListCursorResponseDto;
import sandri.sandriweb.domain.magazine.dto.MagazinePlaceThumbnailDto;
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.place.dto.SimplePlaceDto;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;

import java.util.List;

@RestController
@RequestMapping("/api/magazines")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "매거진", description = "매거진 카드뉴스 관련 API")
public class MagazineController {

    private final MagazineService magazineService;

    /**
     * 사용자 ID 추출 헬퍼 메서드
     */
    private Long extractUserId(User user) {
        return (user != null) ? user.getId() : null;
    }

    /**
     * 예외 처리 헬퍼 메서드 (RuntimeException)
     */
    private <T> ResponseEntity<ApiResponseDto<T>> handleRuntimeException(RuntimeException e, String operation) {
        log.error("{} 실패: {}", operation, e.getMessage());
        if (e.getMessage().contains("찾을 수 없습니다")) {
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponseDto.error(e.getMessage()));
    }

    /**
     * 예외 처리 헬퍼 메서드 (일반 Exception)
     */
    private <T> ResponseEntity<ApiResponseDto<T>> handleException(Exception e, String operation, String errorMessage) {
        log.error("{} 중 오류 발생: ", operation, e);
        return ResponseEntity.badRequest()
                .body(ApiResponseDto.error(errorMessage));
    }

    @GetMapping("/{magazineId}")
    @Operation(summary = "매거진 상세 조회",
               description = "매거진 상세 조회 시 호출합니다." +
                             "매거진 ID, 매거진 내용, 사용자 좋아요 여부, 카드뉴스 개수, 카드뉴스 리스트, 전체 카드뉴스 개수를 반환합니다." +
                             "카드뉴스 리스트에서는 카드뉴스 순서, 카드뉴스 url을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 없음")
    })
    public ResponseEntity<ApiResponseDto<MagazineDetailResponseDto>> getMagazineDetail(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId) {

        log.info("매거진 상세 조회: magazineId={}", magazineId);

        try {
            MagazineDetailResponseDto response = magazineService.getMagazineDetail(magazineId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "매거진 상세 조회");
        } catch (Exception e) {
            return handleException(e, "매거진 상세 조회", "매거진을 조회하는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping
    @Operation(summary = "매거진 목록 조회 (커서 기반 페이징)",
               description = "홈: 여행 매거진 모아보기 페이지에서 호출합니다." +
                             "매거진 목록을 커서 기반(마지막으로 호출한 매거진 ID + 추가로 호출할 매거진 개수를 받아 호출)으로 페이징하여 조회합니다." +
                             "매거진 객체, 반환한 매거진 개수(요청한 수), 마지막으로 호출한 매거진 ID(다음 요청 시 사용), 전체 매거진 개수를 반환합니다." +
                             "매거진 객체에서는 매거진 ID, 매거진 제목, 매거진 썸네일(첫 번째 카드 이미지), 매거진 요약, 매거진 태그, 사용자 좋아요 여부, 마지막으로 조회한 매거진 ID를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<MagazineListCursorResponseDto>> getMagazineList(
            @Parameter(description = "마지막으로 받은 매거진 ID(첫 조회 시 생략)", example = "15")
            @RequestParam(required = false) Long lastMagazineId,
            @Parameter(description = "호출 매거진 목록 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {

        log.info("매거진 목록 조회(커서): lastMagazineId={}, size={}", lastMagazineId, size);

        try {
            Long userId = extractUserId(user);
            sandri.sandriweb.domain.magazine.dto.MagazineListCursorResponseDto response =
                    magazineService.getMagazineListByCursor(lastMagazineId, size, userId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            return handleException(e, "매거진 목록 조회", "매거진 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/{magazineId}/like")
    @Operation(summary = "매거진 좋아요 토글",
               description = "매거진 상세 페이지에서 호출합니다." +
                             "매거진에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 좋아요하지 않은 경우 추가됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 없음")
    })
    public ResponseEntity<ApiResponseDto<Boolean>> toggleLike(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @AuthenticationPrincipal User user) {

        log.info("매거진 좋아요 토글: magazineId={}", magazineId);

        try {
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponseDto.error("로그인이 필요합니다."));
            }

            boolean isLiked = magazineService.toggleLike(magazineId, user.getId());
            String message = isLiked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.";
            return ResponseEntity.ok(ApiResponseDto.success(message, isLiked));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "매거진 좋아요 토글");
        } catch (Exception e) {
            return handleException(e, "매거진 좋아요 토글", "좋아요 처리 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/{magazineId}/places")
    @Operation(summary = "매거진 카드뉴스에 매핑된 장소 목록 조회",
               description = "매거진 상세: 마지막 페이지의 관광지 보러가기 버튼에서 호출합니다." +
                             "매거진 ID를 받아 해당 매거진의 카드뉴스에 연결된 모든 Place를 SimplePlaceDto 리스트로 반환합니다. " +
                             "로그인한 경우 사용자가 좋아요한 장소 여부도 함께 반환됩니다." +
                             "SimplePlaceDto 리스트에서는 장소 ID, 장소 이름, 장소 썸네일, 장소 주소, 장소 카테고리, 사용자 좋아요 여부를 반환합니다. (기준 거리 필드 = null)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 없음")
    })
    public ResponseEntity<ApiResponseDto<List<SimplePlaceDto>>> getPlacesByMagazineId(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @AuthenticationPrincipal User user) {

        log.info("매거진 카드뉴스에 매핑된 장소 목록 조회: magazineId={}", magazineId);

        try {
            Long userId = extractUserId(user);
            List<SimplePlaceDto> response = magazineService.getPlacesByMagazineId(magazineId, userId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "매거진 카드뉴스에 매핑된 장소 목록 조회");
        } catch (Exception e) {
            return handleException(e, "매거진 카드뉴스에 매핑된 장소 목록 조회", "장소 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/{magazineId}/places/thumbnails")
    @Operation(summary = "매거진 카드뉴스에 매핑된 장소 썸네일 목록 조회",
               description = "매거진 상세: 마지막 페이지에서 호출합니다. (관광지 보러가기 버튼 상단)" +
                             "매거진 ID를 받아 해당 매거진의 카드뉴스에 연결된 Place 중 요청된 개수만큼을 반환합니다. " +
                             "장소 ID, 장소 이름, 장소 썸네일을 리스트로 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 없음")
    })
    public ResponseEntity<ApiResponseDto<List<MagazinePlaceThumbnailDto>>> getPlaceThumbnailsByMagazineId(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @Parameter(description = "조회할 개수", example = "3")
            @RequestParam(defaultValue = "3") int count) {

        log.info("매거진 카드뉴스에 매핑된 장소 썸네일 목록 조회: magazineId={}, count={}", magazineId, count);

        try {
            List<MagazinePlaceThumbnailDto> response = magazineService.getPlaceThumbnailsByMagazineId(magazineId, count);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "매거진 카드뉴스에 매핑된 장소 썸네일 목록 조회");
        } catch (Exception e) {
            return handleException(e, "매거진 카드뉴스에 매핑된 장소 썸네일 목록 조회", "장소 썸네일 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }

    
}

