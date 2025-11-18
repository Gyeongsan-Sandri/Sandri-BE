package sandri.sandriweb.domain.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.search.dto.*;
import sandri.sandriweb.domain.search.entity.RecentSearch;
import sandri.sandriweb.domain.search.service.SearchService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "검색 관련 api", description = "장소 및 경로 검색 관련 API")
public class SearchController {

    private final SearchService searchService;
    private final UserRepository userRepository;

    @GetMapping("/places/search")
    @Operation(summary = "장소 검색", 
               description = "키워드로 장소를 검색합니다. 내부 DB를 우선 검색하고, 결과가 부족하면 Google Places API로 보충합니다. " +
                           "DB 장소는 placeId가 있어 상세보기/좋아요 가능하며, Google 보충 결과는 placeId가 null입니다. " +
                           "매거진 장소 매핑 시 이 API로 검색한 후 placeId를 사용하여 매핑 API를 호출하세요.",
               tags = {"검색 관련 api", "매거진 장소매핑"})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<PlaceSearchResponseDto>> searchPlaces(
            @Parameter(description = "검색 키워드", example = "디저트", required = true)
            @RequestParam String keyword,
            @Parameter(description = "카테고리 필터 (선택사항)", example = "식도락")
            @RequestParam(required = false) String category,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        log.info("장소 검색 요청: keyword={}, category={}, page={}, size={}", keyword, category, page, size);

        try {
            Long userId = null;
            if (authentication != null) {
                User user = userRepository.findByUsername(authentication.getName()).orElse(null);
                userId = user != null ? user.getId() : null;
            }

            PlaceSearchResponseDto response = searchService.searchPlaces(keyword, category, page, size, userId);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("장소 검색 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("장소 검색 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/routes/search")
    @Operation(summary = "루트 검색", 
               description = "키워드로 공개된 루트를 검색합니다. 제목, 설명에서 검색합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<RouteSearchResponseDto>> searchRoutes(
            @Parameter(description = "검색 키워드", example = "디저트", required = true)
            @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        log.info("루트 검색 요청: keyword={}, page={}, size={}", keyword, page, size);

        try {
            RouteSearchResponseDto response = searchService.searchRoutes(keyword, page, size);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("루트 검색 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("루트 검색 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/me/recent-searches")
    @Operation(summary = "최근 검색어 조회", 
               description = "현재 로그인한 사용자의 최근 검색어 목록을 조회합니다. 최대 10개까지 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<List<RecentSearchDto>>> getRecentSearches(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("최근 검색어 조회 요청: username={}", username);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            List<RecentSearchDto> response = searchService.getRecentSearches(user.getId());
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("최근 검색어 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("최근 검색어 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/me/recent-searches")
    @Operation(summary = "최근 검색어 추가", 
               description = "검색 키워드를 최근 검색어 목록에 추가합니다. 이미 존재하는 경우 최신순으로 업데이트됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<RecentSearchDto>> addRecentSearch(
            @Valid @RequestBody AddRecentSearchRequestDto request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("최근 검색어 추가 요청: username={}, keyword={}, searchType={}", 
                username, request.getKeyword(), request.getSearchType());

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            RecentSearch.SearchType searchType;
            try {
                searchType = RecentSearch.SearchType.valueOf(request.getSearchType().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("검색 타입은 PLACE 또는 ROUTE여야 합니다."));
            }

            RecentSearchDto response = searchService.addRecentSearch(user.getId(), request.getKeyword(), searchType);
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("최근 검색어 추가 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("최근 검색어 추가 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/me/recent-searches/{searchId}")
    @Operation(summary = "최근 검색어 삭제", 
               description = "특정 최근 검색어를 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "검색어 없음")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteRecentSearch(
            @Parameter(description = "최근 검색어 ID", example = "1")
            @PathVariable Long searchId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("최근 검색어 삭제 요청: username={}, searchId={}", username, searchId);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            searchService.deleteRecentSearch(user.getId(), searchId);
            return ResponseEntity.ok(ApiResponseDto.success(null));
        } catch (RuntimeException e) {
            log.error("최근 검색어 삭제 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(404)
                        .body(ApiResponseDto.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(e.getMessage()));
        } catch (Exception e) {
            log.error("최근 검색어 삭제 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("최근 검색어 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/me/recent-searches/all")
    @Operation(summary = "모든 최근 검색어 삭제", 
               description = "현재 로그인한 사용자의 모든 최근 검색어를 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteAllRecentSearches(
            Authentication authentication) {

        String username = authentication.getName();
        log.info("모든 최근 검색어 삭제 요청: username={}", username);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            searchService.deleteAllRecentSearches(user.getId());
            return ResponseEntity.ok(ApiResponseDto.success(null));
        } catch (Exception e) {
            log.error("모든 최근 검색어 삭제 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("최근 검색어 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/popular-searches")
    @Operation(summary = "인기 검색어 조회", 
               description = "현재 인기 있는 검색어 목록을 조회합니다. 순위와 순위 변동 정보를 포함합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponseDto<List<PopularSearchDto>>> getPopularSearches() {

        log.info("인기 검색어 조회 요청");

        try {
            List<PopularSearchDto> response = searchService.getPopularSearches();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("인기 검색어 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("인기 검색어 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "카테고리 목록 조회", 
               description = "장소 검색에 사용할 수 있는 카테고리 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponseDto<List<CategoryDto>>> getCategories() {

        log.info("카테고리 목록 조회 요청");

        try {
            List<CategoryDto> response = searchService.getCategories();
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("카테고리 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("카테고리 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

