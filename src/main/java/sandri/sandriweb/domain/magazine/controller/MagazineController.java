package sandri.sandriweb.domain.magazine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.magazine.dto.MagazineDetailResponseDto;
import sandri.sandriweb.domain.magazine.dto.MagazineListDto;
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/magazines")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "매거진", description = "매거진 카드뉴스 관련 API")
public class MagazineController {

    private final MagazineService magazineService;
    private final UserRepository userRepository;

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
    @Operation(summary = "매거진 목록 조회",
               description = "매거진 목록을 조회합니다. 제목, 썸네일(첫 번째 카드 이미지), 요약, 좋아요 여부를 반환합니다. 로그인한 경우 사용자가 좋아요한 매거진 여부도 함께 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<MagazineListDto>>> getMagazineList(
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int count,
            Authentication authentication) {

        log.info("매거진 목록 조회: count={}", count);

        try {
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

            List<MagazineListDto> response = magazineService.getMagazineList(count, userId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("매거진 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }
}

