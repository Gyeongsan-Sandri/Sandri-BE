package sandri.sandriweb.domain.visitHistory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.domain.visitHistory.dto.VisitPlaceRequestDto;
import sandri.sandriweb.domain.visitHistory.dto.VisitPlaceResponseDto;
import sandri.sandriweb.domain.visitHistory.service.VisitHistoryService;

@RestController
@RequestMapping("/api/me/visits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "방문 기록", description = "장소 방문 기록 관련 API")
public class VisitHistoryController {

    private final VisitHistoryService visitHistoryService;
    private final UserRepository userRepository;

    @PostMapping("/place/{placeId}")
    @Operation(summary = "장소 방문 확인 및 기록", 
               description = "홈: 오늘 일정: 방문 확정하기에서 호출합니다. " +
                             "사용자의 현재 GPS 위치와 장소 위치의 거리를 계산하여 1km 이내인지 확인합니다. " +
                             "조건에 맞으면 방문 기록을 저장하고 방문 여부를 반환합니다. " +
                             "로그인한 사용자 정보는 자동으로 인식되며, 장소 ID는 경로 변수로, 현재 GPS 위도/경도는 요청 본문으로 입력받습니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "방문 확인 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "방문 성공",
                                            value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"visited\": true,\n    \"visitHistoryId\": 1\n  }\n}"
                                    ),
                                    @ExampleObject(
                                            name = "방문 실패 (거리 초과)",
                                            value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"visited\": false,\n    \"visitHistoryId\": null\n  }\n}"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (좌표 범위 초과, 필수 값 누락 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 장소를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponseDto<VisitPlaceResponseDto>> checkAndRecordVisit(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            @Valid @RequestBody VisitPlaceRequestDto request,
            Authentication authentication) {

        try {
            // 로그인한 사용자 정보 가져오기
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            log.info("장소 방문 확인 요청: userId={}, placeId={}, latitude={}, longitude={}", 
                    user.getId(), placeId, request.getLatitude(), request.getLongitude());

            VisitPlaceResponseDto response = visitHistoryService.checkAndRecordVisit(
                    user,
                    placeId,
                    request.getLatitude(),
                    request.getLongitude()
            );
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("장소 방문 확인 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다") || e.getMessage().contains("비활성화")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장소 방문 확인 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("장소 방문 확인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
