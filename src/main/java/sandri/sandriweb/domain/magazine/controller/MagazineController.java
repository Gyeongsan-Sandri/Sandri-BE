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
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;

@RestController
@RequestMapping("/api/magazines")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "매거진", description = "매거진 카드뉴스 관련 API")
public class MagazineController {

    private final MagazineService magazineService;

    @GetMapping("/{magazineId}")
    @Operation(summary = "매거진 상세 조회",
               description = "매거진 ID로 매거진 정보와 카드뉴스 리스트를 조회합니다.")
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
            log.error("매거진 상세 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("매거진 상세 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진을 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping
    @Operation(summary = "매거진 목록 조회 (커서 기반 페이징)",
               description = "매거진 목록을 커서 기반으로 페이징하여 조회합니다. 제목, 썸네일(첫 번째 카드 이미지), 요약, 좋아요 여부를 반환합니다. 로그인한 경우 사용자가 좋아요한 매거진 여부도 함께 반환됩니다. 마지막으로 조회한 매거진 ID를 기준으로 다음 페이지를 가져옵니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<sandri.sandriweb.domain.magazine.dto.MagazineListCursorResponseDto>> getMagazineList(
            @Parameter(description = "마지막으로 받은 매거진 ID(첫 조회 시 생략)", example = "15")
            @RequestParam(required = false) Long lastMagazineId,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {

        log.info("매거진 목록 조회(커서): lastMagazineId={}, size={}", lastMagazineId, size);

        try {
            // 사용자 ID 조회 (로그인한 경우) - @AuthenticationPrincipal로 최적화
            Long userId = (user != null) ? user.getId() : null;

            sandri.sandriweb.domain.magazine.dto.MagazineListCursorResponseDto response =
                    magazineService.getMagazineListByCursor(lastMagazineId, size, userId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("매거진 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/{magazineId}/like")
    @Operation(summary = "매거진 좋아요 토글",
               description = "매거진에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 좋아요하지 않은 경우 추가됩니다.")
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
            // 인증 확인
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponseDto.error("로그인이 필요합니다."));
            }

            // 좋아요 토글
            boolean isLiked = magazineService.toggleLike(magazineId, user.getId());
            
            String message = isLiked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.";
            return ResponseEntity.ok(ApiResponseDto.success(message, isLiked));

        } catch (RuntimeException e) {
            log.error("매거진 좋아요 토글 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("매거진 좋아요 토글 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("좋아요 처리 중 오류가 발생했습니다."));
        }
    }
}

