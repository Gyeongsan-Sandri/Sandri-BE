package sandri.sandriweb.domain.advertise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sandri.sandriweb.domain.advertise.dto.AdResponseDto;
import sandri.sandriweb.domain.advertise.service.AdvertiseService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;

@RestController
@RequestMapping("/api/advertise")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "광고", description = "광고 조회 관련 API")
public class AdvertiseController {

    private final AdvertiseService advertiseService;

    @GetMapping("/official")
    @Operation(summary = "공식 광고 조회",
               description = "유효한 공식 광고 목록을 조회합니다. enabled=true이고 현재 시간이 노출 기간 내인 광고만 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<AdResponseDto>> getOfficialAds() {
        log.info("공식 광고 조회 요청");

        try {
            AdResponseDto response = advertiseService.getOfficialAds();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("공식 광고 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("공식 광고를 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/private")
    @Operation(summary = "개인 광고 조회",
               description = "유효한 개인 광고 목록을 조회합니다. enabled=true이고 현재 시간이 노출 기간 내인 광고만 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<AdResponseDto>> getPrivateAds() {
        log.info("개인 광고 조회 요청");

        try {
            AdResponseDto response = advertiseService.getPrivateAds();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("개인 광고 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("개인 광고를 조회하는 중 오류가 발생했습니다."));
        }
    }
}

