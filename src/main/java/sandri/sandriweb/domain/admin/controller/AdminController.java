package sandri.sandriweb.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.advertise.dto.CreateOfficialAdRequestDto;
import sandri.sandriweb.domain.advertise.dto.CreatePrivateAdRequestDto;
import sandri.sandriweb.domain.advertise.service.AdvertiseService;
import sandri.sandriweb.domain.magazine.dto.CreateMagazineRequestDto;
import sandri.sandriweb.domain.magazine.dto.UpdateMagazineRequestDto;
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.place.dto.CreatePlacePhotoRequestDto;
import sandri.sandriweb.domain.place.dto.CreatePlaceRequestDto;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관리자", description = "관리자용 데이터 생성 API")
public class AdminController {

    private final PlaceService placeService;
    private final MagazineService magazineService;
    private final AdvertiseService advertiseService;

    // ========== 장소 관련 ==========

    @PostMapping("/places")
    @Operation(summary = "장소 생성",
               description = "관리자가 새로운 장소를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<Long>> createPlace(
            @Valid @RequestBody CreatePlaceRequestDto request) {

        log.info("장소 생성 요청");

        try {
            Long placeId = placeService.createPlace(request);
            return ResponseEntity.ok(ApiResponseDto.success("장소가 생성되었습니다.", placeId));
        } catch (Exception e) {
            log.error("장소 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("장소 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/places/photos")
    @Operation(summary = "장소 사진 추가",
               description = "관리자가 장소에 사진을 추가합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> createPlacePhoto(
            @Valid @RequestBody CreatePlacePhotoRequestDto request) {

        log.info("장소 사진 추가 요청: placeId={}", request.getPlaceId());

        try {
            Long photoId = placeService.createPlacePhoto(request);
            return ResponseEntity.ok(ApiResponseDto.success("장소 사진이 추가되었습니다.", photoId));
        } catch (RuntimeException e) {
            log.error("장소 사진 추가 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장소 사진 추가 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("장소 사진 추가 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ========== 매거진 관련 ==========

    @PostMapping("/magazines")
    @Operation(summary = "매거진 생성",
               description = "관리자가 새로운 매거진을 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<Long>> createMagazine(
            @Valid @RequestBody CreateMagazineRequestDto request) {

        log.info("매거진 생성 요청");

        try {
            Long magazineId = magazineService.createMagazine(request);
            return ResponseEntity.ok(ApiResponseDto.success("매거진이 생성되었습니다.", magazineId));
        } catch (Exception e) {
            log.error("매거진 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/magazines/{magazineId}")
    @Operation(summary = "매거진 수정",
               description = "관리자가 매거진 정보와 카드를 수정합니다. 기존 카드는 모두 삭제되고 새로운 카드로 교체됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> updateMagazine(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @Valid @RequestBody UpdateMagazineRequestDto request) {

        log.info("매거진 수정 요청: magazineId={}", magazineId);

        try {
            Long updatedMagazineId = magazineService.updateMagazine(magazineId, request);
            return ResponseEntity.ok(ApiResponseDto.success("매거진이 수정되었습니다.", updatedMagazineId));
        } catch (RuntimeException e) {
            log.error("매거진 수정 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("매거진 수정 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진 수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ========== 광고 관련 ==========

    @PostMapping("/advertise/official")
    @Operation(summary = "공식 광고 생성",
               description = "관리자가 새로운 공식 광고를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<Long>> createOfficialAd(
            @Valid @RequestBody CreateOfficialAdRequestDto request) {

        log.info("공식 광고 생성 요청");

        try {
            Long adId = advertiseService.createOfficialAd(request);
            return ResponseEntity.ok(ApiResponseDto.success("공식 광고가 생성되었습니다.", adId));
        } catch (Exception e) {
            log.error("공식 광고 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("공식 광고 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/advertise/private")
    @Operation(summary = "개인 광고 생성",
               description = "관리자가 새로운 개인 광고를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<Long>> createPrivateAd(
            @Valid @RequestBody CreatePrivateAdRequestDto request) {

        log.info("개인 광고 생성 요청");

        try {
            Long adId = advertiseService.createPrivateAd(request);
            return ResponseEntity.ok(ApiResponseDto.success("개인 광고가 생성되었습니다.", adId));
        } catch (Exception e) {
            log.error("개인 광고 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("개인 광고 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

