package sandri.sandriweb.domain.dataImport.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
                             "이미 존재하는 장소는 스킵됩니다. " +
                             "주의: 이 작업은 시간이 오래 걸릴 수 있습니다.")
    public ResponseEntity<ApiResponseDto<String>> importPlaces() {
        log.info("장소 데이터 임포트 요청");

        try {
            String result = GBGSDataImportService.importPlacesFromExternalApi();
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
    @Operation(summary = "CSV 파일로 장소 데이터 임포트",
               description = "CSV 파일을 업로드하여 경산시(경상북도) 매장 정보를 DB에 저장합니다. " +
                             "Google Place API로 검색하여 정보를 찾으면 Google 데이터를 우선 사용하고, " +
                             "찾지 못하면 CSV 데이터를 직접 사용하여 저장합니다. " +
                             "이미 존재하는 장소(이름+주소 동일)는 스킵됩니다.")
    public ResponseEntity<ApiResponseDto<String>> importFromCsv(
            @RequestParam("file") MultipartFile file) {
        log.info("CSV 데이터 임포트 요청: filename={}", file.getOriginalFilename());

        try {
            String result = csvImportService.importStoresFromCsv(file);
            return ResponseEntity.ok(ApiResponseDto.success(result));

        } catch (Exception e) {
            log.error("CSV 데이터 임포트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("CSV 임포트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
