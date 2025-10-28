package sandri.sandriweb.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.dto.TelecomCarrierDto;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "공통", description = "공통 데이터 API")
public class CommonController {
    
    @GetMapping("/telecom-carriers")
    @Operation(summary = "통신사 목록 조회", description = "회원가입 시 선택할 수 있는 통신사 목록을 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponseDto<TelecomCarrierDto[]>> getTelecomCarriers() {
        TelecomCarrierDto[] carriers = TelecomCarrierDto.getCarriers();
        ApiResponseDto<TelecomCarrierDto[]> response = ApiResponseDto.success(carriers);
        return ResponseEntity.ok(response);
    }
}
