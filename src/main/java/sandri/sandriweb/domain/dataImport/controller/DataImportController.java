package sandri.sandriweb.domain.dataImport.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.dataImport.service.CsvImportService;
import sandri.sandriweb.domain.dataImport.service.GBGSDataImportService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;

@RestController
@RequestMapping("/api/admin/data-import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "데이터 임포트", description = "외부 API 데이터 임포트 관련 API (관리자 전용)")
public class DataImportController {

    private final GBGSDataImportService GBGSDataImportService;
    private final CsvImportService csvImportService;

    @PostMapping("/gbgs")
    @Operation(summary = "경산시 장소 데이터 임포트",
               description = "경산시 관광 API에서 장소 목록을 가져온 후, Google Place API로 검색하여 DB에 저장합니다. " +
                             "코드 100(음식), 200(숙박), 300(관광명소), 400(문화재/역사)를 모두 순회합니다. " +
                             "mode=insert (기본값): 신규 장소만 추가, 기존 장소는 건드리지 않음. " +
                             "mode=upsert: 신규 추가 + 기존 장소 업데이트. " +
                             "주의: 이 작업은 시간이 오래 걸릴 수 있습니다.")
    public ResponseEntity<ApiResponseDto<String>> importPlaces(
            @RequestParam(value = "mode", defaultValue = "insert") String mode) {
        log.info("장소 데이터 임포트 요청 - mode: {}", mode);

        // mode 검증
        if (!mode.equals("insert") && !mode.equals("upsert")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("mode는 'insert' 또는 'upsert'만 가능합니다."));
        }

        try {
            String result = GBGSDataImportService.importPlacesFromExternalApi(mode);
            return ResponseEntity.ok(ApiResponseDto.success(result));

        } catch (RuntimeException e) {
            // 공공 API 호출 실패 등 명시적인 오류
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("공공 API 호출 실패")) {
                log.error("공공 API 호출 실패: {}", errorMessage);
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error(errorMessage));
            }
            log.error("데이터 임포트 실패: {}", errorMessage);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("데이터 임포트 중 오류가 발생했습니다: " + errorMessage));
        } catch (Exception e) {
            log.error("장소 데이터 임포트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("데이터 임포트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/csv")
    @Operation(
            summary = "CSV 파일로 장소 데이터 임포트",
            description = "서버의 datafile 폴더에 있는 CSV 파일을 읽어 경산시(경상북도) 매장 정보를 DB에 저장합니다.\n\n" +
                         "**처리 과정:**\n" +
                         "1. CSV 파일에서 경산시 매장 정보 추출\n" +
                         "2. Google Place API로 검색하여 장소 정보 조회\n" +
                         "3. Google 데이터가 있으면 우선 사용, 없으면 CSV 데이터 직접 사용\n\n" +
                         "**모드 설명:**\n" +
                         "- `insert` (기본값): 신규 장소만 추가, 기존 장소는 건드리지 않음\n" +
                         "- `upsert`: 신규 추가 + 기존 장소 업데이트\n\n" +
                         "**주의:** 이 작업은 시간이 오래 걸릴 수 있습니다."
    )
    public ResponseEntity<ApiResponseDto<String>> importFromCsv(
            @Parameter(
                    description = "CSV 파일명 (datafile 폴더 내)",
                    required = true,
                    example = "경북_상가정보_시군구코드_47290.csv"
            )
            @RequestParam("fileName") String fileName,

            @Parameter(
                    description = "임포트 모드",
                    schema = @Schema(
                            type = "string",
                            defaultValue = "insert",
                            allowableValues = {"insert", "upsert"}
                    )
            )
            @RequestParam(value = "mode", defaultValue = "insert") String mode,

            jakarta.servlet.http.HttpServletRequest request) {

        // 요청 파라미터 전체 로깅
        log.info("=== 요청 디버깅 시작 ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Query String: {}", request.getQueryString());
        request.getParameterMap().forEach((key, values) -> {
            log.info("Parameter [{}]: {}", key, String.join(", ", values));
        });
        log.info("=== 요청 디버깅 끝 ===");

        log.info("CSV 데이터 임포트 요청: fileName={}, mode={}", fileName, mode);

        // mode 중복 파라미터 처리 (Spring이 쉼표로 합침)
        String normalizedMode = mode.contains(",") ? mode.split(",")[0].trim() : mode.trim();
        if (!normalizedMode.equals(mode)) {
            log.warn("중복된 mode 파라미터 감지: [{}] -> [{}]", mode, normalizedMode);
        }

        // mode 검증
        if (!normalizedMode.equals("insert") && !normalizedMode.equals("upsert")) {
            log.error("잘못된 mode 값: [{}]", normalizedMode);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("mode는 'insert' 또는 'upsert'만 가능합니다."));
        }

        try {
            String result = csvImportService.importStoresFromLocalFile(fileName, normalizedMode);
            return ResponseEntity.ok(ApiResponseDto.success(result));

        } catch (Exception e) {
            log.error("CSV 데이터 임포트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("CSV 임포트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
