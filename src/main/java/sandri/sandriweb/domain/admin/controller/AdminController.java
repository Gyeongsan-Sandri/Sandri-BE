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
import sandri.sandriweb.domain.admin.dto.CreatePlacePhotoRequestDto;
import sandri.sandriweb.domain.admin.dto.CreateTagRequestDto;
import sandri.sandriweb.domain.magazine.dto.CreateMagazineRequestDto;
import sandri.sandriweb.domain.magazine.dto.TagDto;
import sandri.sandriweb.domain.magazine.dto.UpdateMagazineRequestDto;
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.place.dto.CreatePlaceRequestDto;
import sandri.sandriweb.domain.place.dto.PlaceListDto;
import sandri.sandriweb.domain.place.dto.UpdatePlaceRequestDto;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.review.dto.ReviewListDto;
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
               description = "관리자가 새로운 장소를 생성합니다." +
                             "사진은 다른 API를 통해 추가합니다.")
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
               description = "관리자가 장소 정보를 수정합니다. 요청에 포함된 필드만 업데이트되며, 생략된 필드는 기존 값을 유지합니다. " +
                             "사진 정보(photos)가 포함된 경우, order 값에 따라 기존 사진을 업데이트하거나 새로 생성하며, " +
                             "photoUrl이 빈 문자열(\"\")이면 해당 order의 사진을 비활성화합니다. " +
                             "요청에 없는 order의 사진은 유지됩니다 (비활성화되지 않음).")
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
               description = "관리자가 장소에 사진을 여러 장 한번에 추가합니다. " +
                             "사진 정보 리스트(photoUrl과 order)를 받아 사진을 추가합니다. " +
                             "생성된 사진 ID 리스트를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<List<Long>>> createPlacePhotos(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            @Valid @RequestBody CreatePlacePhotoRequestDto request) {

        log.info("장소 사진 추가 요청: placeId={}, photoCount={}", placeId, request.getPhotos() != null ? request.getPhotos().size() : 0);

        try {
            List<Long> photoIds = placeService.createPlacePhotos(placeId, request.getPhotos());
            return ResponseEntity.ok(ApiResponseDto.success("장소 사진이 추가되었습니다.", photoIds));
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
               description = "새로운 매거진을 생성합니다." +
                             "매거진 이름, 매거진 요약, 매거진 내용, 매거진 카드 정보(order와 cardUrl) 리스트를 받아 매거진을 생성합니다." +
                             "태그는 이후 다른 API를 통해 추가합니다.")
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
               description = "관리자가 매거진 정보와 카드를 수정합니다. " +
                             "카드 정보(cards)가 포함된 경우, order 값에 따라 기존 카드를 업데이트하거나 새로 생성하며, " +
                             "cardUrl이 빈 문자열(\"\")이면 해당 order의 카드를 비활성화합니다. " +
                             "요청에 없는 order의 카드는 유지됩니다.")
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

    @GetMapping("/tags")
    @Operation(summary = "태그 목록 조회",
               description = "전체 태그 목록을 조회합니다. 태그 ID와 태그 이름을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<TagDto>>> getAllTags() {
        
        log.info("태그 목록 조회");
        
        try {
            List<TagDto> response = magazineService.getAllTags();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("태그 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("태그 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/tags")
    @Operation(summary = "태그 생성",
               description = "새로운 매거진 태그를 생성합니다. 태그 이름은 중복될 수 없습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<Long>> createTag(
            @Valid @RequestBody CreateTagRequestDto request) {

        log.info("태그 생성 요청: name={}", request.getName());

        try {
            Long tagId = magazineService.createTag(request.getName());
            return ResponseEntity.ok(ApiResponseDto.success("태그가 생성되었습니다.", tagId));
        } catch (RuntimeException e) {
            log.error("태그 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("태그 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("태그 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/magazines/{magazineId}/tags/{tagId}")
    @Operation(summary = "매거진에 태그 추가",
               description = "매거진에 태그를 추가합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 또는 태그 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> addTagToMagazine(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable Long tagId) {

        log.info("매거진에 태그 추가 요청: magazineId={}, tagId={}", magazineId, tagId);

        try {
            Long magazineTagId = magazineService.addTagToMagazine(magazineId, tagId);
            return ResponseEntity.ok(ApiResponseDto.success("매거진에 태그가 추가되었습니다.", magazineTagId));
        } catch (RuntimeException e) {
            log.error("매거진에 태그 추가 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("매거진에 태그 추가 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진에 태그를 추가하는 중 오류가 발생했습니다: " + e.getMessage()));
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
    public ResponseEntity<ApiResponseDto<List<ReviewListDto>>> getAllReviews() {

        log.info("전체 리뷰 목록 조회 (관리자)");

        try {
            List<ReviewListDto> response = reviewService.getAllReviews();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("전체 리뷰 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("전체 리뷰 목록을 조회하는 중 오류가 발생했습니다."));
        }
    }
}

