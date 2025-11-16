package sandri.sandriweb.domain.point.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import sandri.sandriweb.domain.point.dto.ExpiringPointsResponseDto;
import sandri.sandriweb.domain.point.dto.PointHistoryDto;
import sandri.sandriweb.domain.point.enums.PointHistoryType;
import sandri.sandriweb.domain.point.service.PointService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;

import java.util.List;

@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "포인트", description = "포인트 관련 API")
public class PointController {

    private final PointService pointService;

    @GetMapping("/history")
    @Operation(summary = "포인트 히스토리 목록 조회",
               description = "로그인한 사용자의 포인트 적립/사용 내역 목록을 조회합니다. " +
                             "type 파라미터로 전체/적립/사용을 구분할 수 있습니다. " +
                             "- ALL: 전체 조회 (기본값)\n" +
                             "- EARN: 적립 포인트만 조회\n" +
                             "- USE: 사용 포인트만 조회")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<ApiResponseDto<List<PointHistoryDto>>> getPointHistoryList(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "조회 타입 (ALL: 전체, EARN: 적립만, USE: 사용만)",
                       schema = @Schema(defaultValue = "ALL", allowableValues = {"ALL", "EARN", "USE"}))
            @RequestParam(defaultValue = "ALL") PointHistoryType type) {

        log.info("포인트 히스토리 목록 조회 요청: userId={}, type={}", user.getId(), type);

        try {
            List<PointHistoryDto> response = pointService.getUserPointHistoryList(user, type);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("포인트 히스토리 목록 조회 실패: userId={}, type={}, error={}",
                    user.getId(), type, e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("포인트 히스토리 목록 조회 중 오류 발생: userId={}, type={}", user.getId(), type, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("포인트 히스토리를 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/expiring")
    @Operation(summary = "소멸 예정 포인트 조회",
               description = "로그인한 사용자의 7일 이내 소멸 예정 포인트를 조회합니다. " +
                             "포인트는 적립 후 30일이 지나면 소멸됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<ApiResponseDto<ExpiringPointsResponseDto>> getExpiringPoints(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {

        log.info("소멸 예정 포인트 조회 요청: userId={}", user.getId());

        try {
            ExpiringPointsResponseDto response = pointService.getExpiringPoints(user);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("소멸 예정 포인트 조회 실패: userId={}, error={}", user.getId(), e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("소멸 예정 포인트 조회 중 오류 발생: userId={}", user.getId(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("소멸 예정 포인트를 조회하는 중 오류가 발생했습니다."));
        }
    }
}
