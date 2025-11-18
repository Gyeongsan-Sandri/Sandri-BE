package sandri.sandriweb.domain.admin.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sandri.sandriweb.domain.advertise.dto.CreateOfficialAdRequestDto;
import sandri.sandriweb.domain.advertise.dto.CreatePrivateAdRequestDto;
import sandri.sandriweb.domain.advertise.service.AdvertiseService;
import sandri.sandriweb.domain.admin.dto.CreatePlacePhotoRequestDto;
import sandri.sandriweb.domain.admin.dto.CreateTagRequestDto;
import sandri.sandriweb.domain.admin.dto.MapPlaceByNameRequestDto;
import sandri.sandriweb.domain.magazine.dto.CreateMagazineRequestDto;
import sandri.sandriweb.domain.magazine.dto.TagDto;
import sandri.sandriweb.domain.magazine.dto.UpdateMagazineRequestDto;
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.place.dto.CreatePlaceRequestDto;
import sandri.sandriweb.domain.place.dto.CreatePlaceFormRequestDto;
import sandri.sandriweb.domain.place.dto.PlaceListDto;
import sandri.sandriweb.domain.place.dto.UpdatePlaceRequestDto;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.point.dto.CreatePointEarnConditionRequestDto;
import sandri.sandriweb.domain.point.service.PointService;
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
    private final PointService pointService;

    // ========== 장소 관련 ==========

    @PostMapping(value = "/places", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(hidden = true)
    public ResponseEntity<ApiResponseDto<Long>> createPlaceJson(
            @Valid @RequestBody CreatePlaceRequestDto request) {
        return handleCreatePlace(request, null);
    }

    @PostMapping(value = "/places", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "장소 생성",
               description = "Swagger 폼으로 장소를 생성합니다. 주소만 입력하면 Google Geocoding API로 위도/경도가 자동 계산되며, " +
                             "`photos` 필드에 이미지 파일을 첨부하면 자동으로 S3에 업로드되어 장소 대표 사진으로 등록됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CreatePlaceFormRequestDto.class))
    )
    public ResponseEntity<ApiResponseDto<Long>> createPlaceForm(
            @Valid @ModelAttribute CreatePlaceFormRequestDto request) {

        return handleCreatePlace(request, request.getPhotos());
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
               description = "JSON 형식으로 매거진을 생성합니다. " +
                             "cards 배열에는 order와 cardUrl을 포함한 카드 정보를 입력하세요. " +
                             "태그는 추후 별도 API로 연결합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<Long>> createMagazine(
            @Valid @RequestBody CreateMagazineRequestDto request) {
        return handleCreateMagazine(request);
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

    @PutMapping("/tags/{tagId}")
    @Operation(summary = "태그 수정",
               description = "기존 매거진 태그의 이름을 수정합니다. 태그 이름은 중복될 수 없습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "태그 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> updateTag(
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable Long tagId,
            @Valid @RequestBody CreateTagRequestDto request) {

        log.info("태그 수정 요청: tagId={}, name={}", tagId, request.getName());

        try {
            Long updatedTagId = magazineService.updateTag(tagId, request.getName());
            return ResponseEntity.ok(ApiResponseDto.success("태그가 수정되었습니다.", updatedTagId));
        } catch (RuntimeException e) {
            log.error("태그 수정 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("태그 수정 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("태그 수정 중 오류가 발생했습니다: " + e.getMessage()));
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

    @DeleteMapping("/magazines/{magazineId}/tags/{tagId}")
    @Operation(summary = "매거진에서 태그 삭제",
               description = "매거진에서 태그 매핑을 해제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진, 태그 또는 매핑 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> removeTagFromMagazine(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable Long tagId) {

        log.info("매거진에서 태그 삭제 요청: magazineId={}, tagId={}", magazineId, tagId);

        try {
            Long magazineTagId = magazineService.removeTagFromMagazine(magazineId, tagId);
            return ResponseEntity.ok(ApiResponseDto.success("매거진에서 태그가 삭제되었습니다.", magazineTagId));
        } catch (RuntimeException e) {
            log.error("매거진에서 태그 삭제 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다") || e.getMessage().contains("추가되어 있지 않습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("매거진에서 태그 삭제 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진에서 태그를 삭제하는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/magazines/{magazineId}/cards/{cardOrder}/place/{placeId}")
    @Operation(summary = "매거진 카드에 장소 매핑 (장소 ID로)",
               description = "장소 검색 API(`GET /api/places/search`)로 검색한 결과에서 선택한 장소를 매거진 카드에 매핑합니다. " +
                             "일대일 매핑이므로 기존 매핑이 있으면 새로운 장소로 교체됩니다.",
               tags = {"매거진 장소매핑"})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매핑 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 카드 또는 장소 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> mapPlaceToCard(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @Parameter(description = "카드 순서 (0부터 시작)", example = "0")
            @PathVariable Integer cardOrder,
            @Parameter(description = "장소 ID (장소 검색 API로 검색한 결과의 placeId)", example = "37")
            @PathVariable Long placeId) {

        log.info("매거진 카드에 장소 매핑 요청: magazineId={}, cardOrder={}, placeId={}", 
                 magazineId, cardOrder, placeId);

        try {
            // 매거진 카드에 Place 매핑
            Long updatedCardId = magazineService.mapPlaceToCard(magazineId, cardOrder, placeId);
            return ResponseEntity.ok(ApiResponseDto.success("매거진 카드에 장소가 매핑되었습니다.", updatedCardId));
        } catch (RuntimeException e) {
            log.error("매거진 카드에 장소 매핑 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("매거진 카드에 장소 매핑 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진 카드에 장소를 매핑하는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/magazines/{magazineId}/cards/{cardOrder}/place")
    @Operation(summary = "매거진 카드에서 장소 매핑 해제",
               description = "매거진 카드에서 매핑된 장소를 해제합니다. " +
                             "카드는 매거진 ID와 order로 식별됩니다.",
               tags = {"매거진 장소매핑"})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매핑 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매거진 카드 없음")
    })
    public ResponseEntity<ApiResponseDto<Long>> unmapPlaceFromCard(
            @Parameter(description = "매거진 ID", example = "1")
            @PathVariable Long magazineId,
            @Parameter(description = "카드 순서 (0부터 시작)", example = "0")
            @PathVariable Integer cardOrder) {

        log.info("매거진 카드에서 장소 매핑 해제 요청: magazineId={}, cardOrder={}", magazineId, cardOrder);

        try {
            Long updatedCardId = magazineService.mapPlaceToCard(magazineId, cardOrder, null);
            return ResponseEntity.ok(ApiResponseDto.success("매거진 카드에서 장소 매핑이 해제되었습니다.", updatedCardId));
        } catch (RuntimeException e) {
            log.error("매거진 카드에서 장소 매핑 해제 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("매거진 카드에서 장소 매핑 해제 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진 카드에서 장소 매핑을 해제하는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ========== 광고 관련 ==========

    @PostMapping(value = "/advertise/official", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(hidden = true)
    public ResponseEntity<ApiResponseDto<Long>> createOfficialAdJson(
            @Valid @RequestBody CreateOfficialAdRequestDto request) {
        return handleCreateOfficialAd(request);
    }

    @PostMapping(value = "/advertise/official", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "공식 광고 생성",
               description = "Swagger 폼 입력으로 공식 광고를 생성합니다. 이미지 URL, 노출 기간, 링크 등을 각각 입력하면 됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CreateOfficialAdRequestDto.class))
    )
    public ResponseEntity<ApiResponseDto<Long>> createOfficialAdForm(
            @Valid @ModelAttribute CreateOfficialAdRequestDto request) {

        return handleCreateOfficialAd(request);
    }

    @PostMapping(value = "/advertise/private", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(hidden = true)
    public ResponseEntity<ApiResponseDto<Long>> createPrivateAdJson(
            @Valid @RequestBody CreatePrivateAdRequestDto request) {
        return handleCreatePrivateAd(request);
    }

    @PostMapping(value = "/advertise/private", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "개인 광고 생성",
               description = "Swagger 폼 입력으로 개인 광고를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CreatePrivateAdRequestDto.class))
    )
    public ResponseEntity<ApiResponseDto<Long>> createPrivateAdForm(
            @Valid @ModelAttribute CreatePrivateAdRequestDto request) {

        return handleCreatePrivateAd(request);
    }

    @GetMapping("/places")
    @Operation(summary = "전체 장소 목록 조회 (간단 정보, 관리자용)",
               description = "관리자가 전체 관광지의 ID와 이름만 조회합니다. " +
                             "전체 DB 목록 확인용입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<PlaceListDto>>> getAllPlaces() {

        log.info("전체 장소 목록 조회 (관리자)");

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

    @GetMapping("/reviews")
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

    // ========== 포인트 관련 ==========

    @PostMapping("/point/conditions")
    @Operation(summary = "포인트 적립 조건 등록/수정",
               description = "포인트 적립 조건을 등록하거나 수정합니다. " +
                             "조건 타입(conditionType)은 unique하므로, 이미 존재하는 조건 타입이면 포인트 양만 업데이트되고, " +
                             "존재하지 않는 조건 타입이면 새로 생성됩니다. " +
                             "(현재 conditionType: SIGN_UP, REFERRAL, PLACE_VISIT, REVIEW_CREATE, REVIEW_WITH_PHOTO, PROFILE_COMPLETE) " +
                             "pointAmount는 0 이상의 값이어야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "등록/수정 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "새로운 조건 생성",
                                            value = "{\n  \"success\": true,\n  \"message\": \"포인트 적립 조건이 등록/수정되었습니다.\",\n  \"data\": 1\n}"
                                    ),
                                    @ExampleObject(
                                            name = "기존 조건 수정",
                                            value = "{\n  \"success\": true,\n  \"message\": \"포인트 적립 조건이 등록/수정되었습니다.\",\n  \"data\": 1\n}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "필수 필드 누락",
                                            value = "{\n  \"success\": false,\n  \"message\": \"적립 조건 타입은 필수입니다\",\n  \"data\": null\n}"
                                    ),
                                    @ExampleObject(
                                            name = "포인트 양 유효성 검증 실패",
                                            value = "{\n  \"success\": false,\n  \"message\": \"적립 포인트는 0 이상이어야 합니다\",\n  \"data\": null\n}"
                                    )
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreatePointEarnConditionRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "리뷰 포인트 조건 생성",
                                    value = "{\n  \"conditionType\": \"REVIEW_CREATE\",\n  \"pointAmount\": 100\n}"
                            ),
                            @ExampleObject(
                                    name = "사진 리뷰 포인트 조건 생성",
                                    value = "{\n  \"conditionType\": \"REVIEW_WITH_PHOTO\",\n  \"pointAmount\": 200\n}"
                            ),
                            @ExampleObject(
                                    name = "방문 포인트 조건 수정",
                                    value = "{\n  \"conditionType\": \"PLACE_VISIT\",\n  \"pointAmount\": 500\n}"
                            )
                    }
            )
    )
    public ResponseEntity<ApiResponseDto<Long>> createOrUpdatePointEarnCondition(
            @Valid @RequestBody CreatePointEarnConditionRequestDto request) {

        log.info("포인트 적립 조건 등록/수정 요청: conditionType={}, pointAmount={}",
                request.getConditionType(), request.getPointAmount());

        try {
            Long conditionId = pointService.createOrUpdateEarnCondition(
                    request.getConditionType(),
                    request.getPointAmount()
            );
            return ResponseEntity.ok(ApiResponseDto.success("포인트 적립 조건이 등록/수정되었습니다.", conditionId));
        } catch (Exception e) {
            log.error("포인트 적립 조건 등록/수정 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("포인트 적립 조건 등록/수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ========== 공통 핸들러 ==========

    private ResponseEntity<ApiResponseDto<Long>> handleCreatePlace(CreatePlaceRequestDto request, List<MultipartFile> photos) {
        log.info("장소 생성 요청: name={}", request.getName());
        try {
            Long placeId = placeService.createPlace(request, photos);
            return ResponseEntity.ok(ApiResponseDto.success("장소가 생성되었습니다.", placeId));
        } catch (Exception e) {
            log.error("장소 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("장소 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponseDto<Long>> handleCreateMagazine(CreateMagazineRequestDto request) {
        log.info("매거진 생성 요청: name={}", request.getName());
        try {
            Long magazineId = magazineService.createMagazine(request);
            return ResponseEntity.ok(ApiResponseDto.success("매거진이 생성되었습니다.", magazineId));
        } catch (Exception e) {
            log.error("매거진 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("매거진 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponseDto<Long>> handleCreateOfficialAd(CreateOfficialAdRequestDto request) {
        log.info("공식 광고 생성 요청: title={}", request.getTitle());
        try {
            Long adId = advertiseService.createOfficialAd(request);
            return ResponseEntity.ok(ApiResponseDto.success("공식 광고가 생성되었습니다.", adId));
        } catch (Exception e) {
            log.error("공식 광고 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("공식 광고 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponseDto<Long>> handleCreatePrivateAd(CreatePrivateAdRequestDto request) {
        log.info("개인 광고 생성 요청: title={}", request.getTitle());
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

