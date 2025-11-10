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
import sandri.sandriweb.domain.place.dto.PlaceListDto;
import sandri.sandriweb.domain.place.dto.UpdatePlaceRequestDto;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.review.dto.AdminReviewListDto;
import sandri.sandriweb.domain.review.service.ReviewService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관리자", description = "관리자용 데이터 생성 API")
public class AdminController {

    private final PlaceService placeService;
    private final MagazineService magazineService;
    private final AdvertiseService advertiseService;
    private final ReviewService reviewService;

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

    @PutMapping("/places/{placeId}")
    @Operation(summary = "장소 수정",
               description = "관리자가 장소 정보를 수정합니다. 요청에 포함된 필드만 업데이트되며, 생략된 필드는 기존 값을 유지합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> updatePlace(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            @Valid @RequestBody UpdatePlaceRequestDto request) {

        log.info("장소 수정 요청: placeId={}", placeId);

        try {
            Long updatedPlaceId = placeService.updatePlace(placeId, request);
            return ResponseEntity.ok(ApiResponseDto.success("장소가 수정되었습니다.", updatedPlaceId));
        } catch (RuntimeException e) {
            log.error("장소 수정 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장소 수정 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("장소 수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/places/{placeId}/photos")
    @Operation(summary = "장소 사진 추가",
               description = "관리자가 장소에 사진을 추가합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> createPlacePhoto(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            @Valid @RequestBody CreatePlacePhotoRequestDto request) {

        log.info("장소 사진 추가 요청: placeId={}", placeId);

        try {
            Long photoId = placeService.createPlacePhoto(placeId, request.getPhotoUrl());
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

    @GetMapping("/places/list")
    @Operation(summary = "전체 장소 목록 조회 (간단 정보)",
               description = "전체 관광지의 ID와 이름만 반환합니다." +
                             "전체 DB 목록 확인용")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<PlaceListDto>>> getPlaceLists() {

        log.info("전체 장소 목록 조회");

        try {
            List<PlaceListDto> response = placeService.getAllPlaces();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("전체 장소 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("전체 장소 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }

    // ========== 리뷰 관련 ==========

    @GetMapping("/reviews/list")
    @Operation(summary = "전체 리뷰 목록 조회 (관리자용)",
               description = "관리자가 전체 리뷰 목록을 조회합니다. 리뷰 ID와 리뷰 내용만 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<AdminReviewListDto>>> getAllReviews() {

        log.info("전체 리뷰 목록 조회 (관리자)");

        try {
            List<AdminReviewListDto> response = reviewService.getAllReviews();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("전체 리뷰 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("전체 리뷰 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }
}

