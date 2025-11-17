package sandri.sandriweb.domain.dataImport.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sandri.sandriweb.domain.dataImport.service.DataImportService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;

@RestController
@RequestMapping("/api/admin/data-import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "데이터 임포트", description = "외부 API 데이터 임포트 관련 API (관리자 전용)")
public class DataImportController {

    private final DataImportService dataImportService;

    @PostMapping("/gbgs")
    @Operation(summary = "경산시 장소 데이터 임포트",
               description = "경산시 관광 API에서 장소 목록을 가져온 후, Google Place API로 검색하여 DB에 저장합니다. " +
                             "코드 100(음식), 200(숙박), 300(관광명소), 400(문화재/역사)를 모두 순회합니다. " +
                             "이미 존재하는 장소는 스킵됩니다. " +
                             "주의: 이 작업은 시간이 오래 걸릴 수 있습니다.")
    public ResponseEntity<ApiResponseDto<String>> importPlaces() {
        log.info("장소 데이터 임포트 요청");

        try {
            String result = dataImportService.importPlacesFromExternalApi();
            return ResponseEntity.ok(ApiResponseDto.success(result));

        } catch (Exception e) {
            log.error("장소 데이터 임포트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("데이터 임포트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
