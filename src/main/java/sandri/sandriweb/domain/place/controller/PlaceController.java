package sandri.sandriweb.domain.place.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.place.dto.SimplePlaceDto;
import sandri.sandriweb.domain.place.dto.NearbyPlaceDto;
import sandri.sandriweb.domain.place.dto.PlaceDetailResponseDto;
import sandri.sandriweb.domain.place.dto.HotPlaceDto;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관광지", description = "관광지 정보 관련 API")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/{placeId}")
    @Operation(summary = "관광지 상세 정보 조회", 
               description = "관광지의 기본 정보를 조회합니다. 이름, 주소, 평점, 카테고리, 공식 사진을 반환합니다. " +
                           "리뷰 정보는 /api/places/{placeId}/reviews API를 별도로 호출하세요.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"placeId\": 1,\n    \"name\": \"경주 불국사\",\n    \"groupName\": \"관광지\",\n    \"categoryName\": \"역사/전통\",\n    \"rating\": 4.5,\n    \"address\": \"경상북도 경주시 불국로 385\",\n    \"latitude\": 35.7894,\n    \"longitude\": 129.3320,\n    \"summary\": \"신라 불교 문화의 정수를 보여주는 사찰\",\n    \"officialPhotos\": [\n      {\n        \"order\": 0,\n        \"photoUrl\": \"https://s3.../photo1.jpg\"\n      }\n    ]\n  }\n}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<PlaceDetailResponseDto>> getPlaceDetail(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId) {

        log.info("관광지 상세 정보 조회: placeId={}", placeId);

        try {
            PlaceDetailResponseDto response = placeService.getPlaceDetail(placeId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("관광지 상세 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("관광지 상세 정보 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("관광지 정보를 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{placeId}/nearby")
    @Operation(summary = "근처 가볼만한 곳 조회 (거리 순)", 
               description = "지도 아래 주변 탐색 버튼을 눌렀을 때 출력할 관광지를 조회합니다. " +
                             "특정 관광지 근처의 추천 장소 목록을 대분류 필터 없이 전체에서 가까운 순으로 조회합니다. count 값이 없을 시 기본값으로 조회함. (근데 거리 제한 있어야 하나 고민중임.)" +
                             "관광지 ID, 이름, 대표 사진 한 장, 위도/경도(지도 출력용), 현재 장소로부터 가까운 순위를 반환합니다. (0일 경우 현재 장소)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<List<NearbyPlaceDto>>> getNearbyPlaces(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId,
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int count) {

        log.info("근처 장소 조회 (거리 순): placeId={}, count={}", placeId, count);

        try {
            List<NearbyPlaceDto> response = placeService.getNearbyPlaces(placeId, count);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("근처 장소 조회 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("근처 장소 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("근처 장소를 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{placeId}/nearby/group")
    @Operation(summary = "근처 가볼만한 곳 조회 (대분류별, 좋아요 순)", 
               description = "관광지 상세페이지 하단의 이 근처의 가볼만한 곳에서 호출하여 사용합니다. " +
                             "현재 관광지에서 10km 이내이고 대분류에 속하는 관광지 중 좋아요가 높은 관광지 6개를 반환합니다. 예시에 count 값이 6으로 되어있는데 count 값이 없을 시 기본값으로 조회함. (근데 10km인 거 애매하긴 함. 수정 고려중.) " +
                             "3개씩 출력하게 되어있을텐데 원래는 버튼 누를 때마다 API 호출해야 하지만 편의상 6개를 한 번에 받고 3개씩 출력해주세요. " +
                             "이 방식이 곤란하면 변경 요청 가능.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관광지 없음")
    })
    public ResponseEntity<ApiResponseDto<List<NearbyPlaceDto>>> getNearbyPlacesByGroup(
            @Parameter(description = "관광지 ID", example = "1")
            @PathVariable Long placeId,
            @Parameter(description = "대분류 (관광지/맛집/카페)", example = "맛집")
            @RequestParam String group,
            @Parameter(description = "조회할 개수", example = "6")
            @RequestParam(defaultValue = "6") int count) {

        log.info("근처 장소 조회 (대분류별, 좋아요 순): placeId={}, group={}, count={}", placeId, group, count);

        try {
            List<NearbyPlaceDto> response = placeService.getNearbyPlacesByGroup(placeId, group, count);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("근처 장소 조회 실패: {}", e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("근처 장소 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("근처 장소를 조회하는 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("")
    @Operation(summary = "카테고리별 장소 조회",
               description = "홈: 카테고리 버튼을 눌러 나오는 카테고리별 장소 조회에서 호출하여 사용합니다. " +
               "카테고리별로 좋아요가 많은 순으로 장소 목록을 조회합니다. 로그인한 경우 사용자가 좋아요한 장소 여부도 함께 반환됩니다. " +
               "관광지 ID, 이름, 주소, 대표 사진 한 장, 사용자가 좋아요한 장소인지 여부, 대분류, 카테고리를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<SimplePlaceDto>>> getPlacesByCategory(
            @Parameter(description = "카테고리 ('자연/힐링', '역사/전통', '문화/체험', '식도락' 중 하나)", example = "자연/힐링")
            @RequestParam String category,
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int count,
            @AuthenticationPrincipal User user) {

        log.info("카테고리별 장소 조회: category={}, count={}", category, count);

        try {
            // 사용자 ID 조회 (로그인한 경우) - @AuthenticationPrincipal로 최적화
            Long userId = (user != null) ? user.getId() : null;

            List<SimplePlaceDto> response = placeService.getPlacesByCategory(category, count, userId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (RuntimeException e) {
            log.error("카테고리별 장소 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("카테고리별 장소 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("카테고리별 장소를 조회하는 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/{placeId}/like")
    @Operation(summary = "장소 좋아요 토글",
               description = "관광지 상세 페이지에서 호출합니다." +
                             "장소에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 아닌 경우 추가됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소 없음")
    })
    public ResponseEntity<ApiResponseDto<Boolean>> toggleLike(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            @AuthenticationPrincipal User user) {

        log.info("장소 좋아요 토글: placeId={}", placeId);

        try {
            // 인증 확인
            if (user == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponseDto.error("로그인이 필요합니다."));
            }

            // 좋아요 토글
            boolean isLiked = placeService.toggleLike(placeId, user.getId());
            
            String message = isLiked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.";
            return ResponseEntity.ok(ApiResponseDto.success(message, isLiked));

        } catch (RuntimeException e) {
            log.error("장소 좋아요 토글 실패: {}", e.getMessage());
            // 예외 메시지에 따라 적절한 HTTP 상태 코드 반환
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("장소 좋아요 토글 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("좋아요 처리 중 오류가 발생했습니다."));
        }
    }


    @GetMapping("/hot")
    @Operation(summary = "HOT 관광지 조회",
            description = "좋아요 수와 최근 활동 가중치를 기반으로 인기 관광지를 순위대로 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<List<HotPlaceDto>>> getHotPlaces() {

        List<HotPlaceDto> hotPlaces = placeService.getHotPlaces(5);
        return ResponseEntity.ok(ApiResponseDto.success(hotPlaces));
    }
}

