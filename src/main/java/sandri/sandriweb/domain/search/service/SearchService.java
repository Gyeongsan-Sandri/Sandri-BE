package sandri.sandriweb.domain.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.entity.PlacePhoto;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.repository.PlacePhotoRepository;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.place.repository.UserPlaceRepository;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.repository.RouteRepository;
import sandri.sandriweb.domain.search.dto.*;
import sandri.sandriweb.domain.search.entity.PopularSearch;
import sandri.sandriweb.domain.search.entity.RecentSearch;
import sandri.sandriweb.domain.search.entity.SearchLog;
import sandri.sandriweb.domain.search.repository.PopularSearchRepository;
import sandri.sandriweb.domain.search.repository.RecentSearchRepository;
import sandri.sandriweb.domain.search.repository.SearchLogRepository;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PlaceRepository placeRepository;
    private final RouteRepository routeRepository;
    private final RecentSearchRepository recentSearchRepository;
    private final SearchLogRepository searchLogRepository;
    private final PopularSearchRepository popularSearchRepository;
    private final PlacePhotoRepository placePhotoRepository;
    private final UserPlaceRepository userPlaceRepository;
    private final UserRepository userRepository;

    private static final int MAX_RECENT_SEARCHES = 10;

    /**
     * 장소 검색
     */
    @Transactional
    public PlaceSearchResponseDto searchPlaces(String keyword, String category, int page, int size, Long userId) {
        // 검색 로그 저장
        saveSearchLog(keyword, SearchLog.SearchType.PLACE);
        
        Pageable pageable = PageRequest.of(page - 1, size);
        
        Page<Place> placePage;
        if (category != null && !category.isEmpty()) {
            // 카테고리 필터 포함 검색
            placePage = placeRepository.searchByKeywordAndCategory(keyword, category, pageable);
        } else {
            // 일반 검색
            placePage = placeRepository.searchByKeyword(keyword, pageable);
        }

        List<Place> places = placePage.getContent();
        List<Long> placeIds = places.stream().map(Place::getId).collect(Collectors.toList());

        // 사진 URL 조회 (배치)
        final Map<Long, String> photoUrlMap;
        if (!placeIds.isEmpty()) {
            List<Object[]> photoResults = placePhotoRepository.findFirstPhotoUrlByPlaceIdIn(placeIds);
            photoUrlMap = photoResults.stream()
                    .collect(Collectors.toMap(
                            result -> ((Number) result[0]).longValue(),
                            result -> (String) result[1]
                    ));
        } else {
            photoUrlMap = new HashMap<>();
        }

        // DTO 변환
        List<PlaceSearchResponseDto.PlaceSearchItemDto> items = places.stream()
                .map(place -> {
                    // 해시태그 생성 (카테고리 기반)
                    List<String> hashtags = generateHashtags(place);
                    
                    return PlaceSearchResponseDto.PlaceSearchItemDto.builder()
                            .placeId(place.getId())
                            .name(place.getName())
                            .address(place.getAddress())
                            .thumbnailUrl(photoUrlMap.getOrDefault(place.getId(), null))
                            .rating(null) // 평점은 리뷰 서비스에서 가져와야 함
                            .likeCount(0) // 좋아요 수는 별도 조회 필요
                            .groupName(place.getGroup() != null ? place.getGroup().name() : null)
                            .categoryName(place.getCategory() != null ? place.getCategory().getDisplayName() : null)
                            .hashtags(hashtags)
                            .build();
                })
                .collect(Collectors.toList());

        return PlaceSearchResponseDto.builder()
                .places(items)
                .totalCount(placePage.getTotalElements())
                .page(page)
                .size(size)
                .hasNext(placePage.hasNext())
                .build();
    }

    /**
     * 루트 검색
     */
    @Transactional
    public RouteSearchResponseDto searchRoutes(String keyword, int page, int size) {
        // 검색 로그 저장
        saveSearchLog(keyword, SearchLog.SearchType.ROUTE);
        
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Route> routePage = routeRepository.searchByKeyword(keyword, pageable);

        List<Route> routes = routePage.getContent();

        // DTO 변환
        List<RouteSearchResponseDto.RouteSearchItemDto> items = routes.stream()
                .map(route -> {
                    // 대표 이미지: 첫 번째 RouteLocation의 name으로 Place를 찾아서 첫 번째 사진 사용
                    String thumbnailUrl = getRouteThumbnail(route);

                    // 해시태그 생성 (카테고리 기반, 루트의 경우 설명에서 추출하거나 기본값)
                    List<String> hashtags = generateRouteHashtags(route);

                    return RouteSearchResponseDto.RouteSearchItemDto.builder()
                            .routeId(route.getId())
                            .title(route.getTitle())
                            .startDate(route.getStartDate())
                            .endDate(route.getEndDate())
                            .creatorNickname(route.getCreator().getNickname())
                            .thumbnailUrl(thumbnailUrl)
                            .hashtags(hashtags)
                            .build();
                })
                .collect(Collectors.toList());

        return RouteSearchResponseDto.builder()
                .routes(items)
                .totalCount(routePage.getTotalElements())
                .page(page)
                .size(size)
                .hasNext(routePage.hasNext())
                .build();
    }

    /**
     * 최근 검색어 조회
     */
    @Transactional(readOnly = true)
    public List<RecentSearchDto> getRecentSearches(Long userId) {
        List<RecentSearch> recentSearches = recentSearchRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // 최대 10개만 반환
        return recentSearches.stream()
                .limit(MAX_RECENT_SEARCHES)
                .map(RecentSearchDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 최근 검색어 추가
     */
    @Transactional
    public RecentSearchDto addRecentSearch(Long userId, String keyword, RecentSearch.SearchType searchType) {
        // 기존 검색어가 있으면 삭제하고 새로 추가 (최신순 유지)
        recentSearchRepository.findByUserIdAndKeywordAndSearchTypeAndEnabledTrue(userId, keyword, searchType)
                .ifPresent(recentSearch -> {
                    recentSearch.disable();
                    recentSearchRepository.save(recentSearch);
                });

        // 새 검색어 추가
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        RecentSearch recentSearch = RecentSearch.builder()
                .user(user)
                .keyword(keyword)
                .searchType(searchType)
                .build();

        RecentSearch saved = recentSearchRepository.save(recentSearch);

        // 최대 개수 초과 시 오래된 것 삭제
        List<RecentSearch> allRecentSearches = recentSearchRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (allRecentSearches.size() > MAX_RECENT_SEARCHES) {
            List<RecentSearch> toDelete = allRecentSearches.subList(MAX_RECENT_SEARCHES, allRecentSearches.size());
            toDelete.forEach(RecentSearch::disable);
            recentSearchRepository.saveAll(toDelete);
        }

        return RecentSearchDto.from(saved);
    }

    /**
     * 최근 검색어 삭제
     */
    @Transactional
    public void deleteRecentSearch(Long userId, Long searchId) {
        RecentSearch recentSearch = recentSearchRepository.findById(searchId)
                .orElseThrow(() -> new RuntimeException("최근 검색어를 찾을 수 없습니다."));

        if (!recentSearch.getUser().getId().equals(userId)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        recentSearch.disable();
        recentSearchRepository.save(recentSearch);
    }

    /**
     * 모든 최근 검색어 삭제
     */
    @Transactional
    public void deleteAllRecentSearches(Long userId) {
        recentSearchRepository.deleteAllByUserId(userId);
    }

    /**
     * 인기 검색어 조회
     */
    @Transactional(readOnly = true)
    public List<PopularSearchDto> getPopularSearches() {
        List<PopularSearch> popularSearches = popularSearchRepository.findLatestPopularSearches();
        
        // 집계된 데이터가 있으면 반환
        if (!popularSearches.isEmpty() && popularSearches.size() >= 5) {
            return popularSearches.stream()
                    .limit(5)
                    .map(ps -> {
                        String rankChange = "SAME";
                        if (ps.getPreviousRank() != null) {
                            if (ps.getRank() < ps.getPreviousRank()) {
                                rankChange = "UP";
                            } else if (ps.getRank() > ps.getPreviousRank()) {
                                rankChange = "DOWN";
                            }
                        }
                        
                        return PopularSearchDto.builder()
                                .rank(ps.getRank())
                                .keyword(ps.getKeyword())
                                .rankChange(rankChange)
                                .previousRank(ps.getPreviousRank())
                                .build();
                    })
                    .collect(Collectors.toList());
        }
        
        // 집계된 데이터가 없으면 하드코딩된 기본값 반환
        return Arrays.asList(
                PopularSearchDto.builder().rank(1).keyword("문천지").rankChange("UP").previousRank(2).build(),
                PopularSearchDto.builder().rank(2).keyword("팔공산 자락 계곡").rankChange("UP").previousRank(3).build(),
                PopularSearchDto.builder().rank(3).keyword("남매지").rankChange("DOWN").previousRank(2).build(),
                PopularSearchDto.builder().rank(4).keyword("경산 자연 마당").rankChange("UP").previousRank(5).build(),
                PopularSearchDto.builder().rank(5).keyword("삼성산").rankChange("DOWN").previousRank(4).build()
        );
    }

    /**
     * 카테고리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories() {
        return Arrays.stream(Category.values())
                .map(category -> CategoryDto.builder()
                        .code(category.name())
                        .name(category.getDisplayName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 검색 로그 저장
     */
    private void saveSearchLog(String keyword, SearchLog.SearchType searchType) {
        try {
            SearchLog searchLog = SearchLog.builder()
                    .keyword(keyword)
                    .searchType(searchType)
                    .searchedAt(LocalDateTime.now())
                    .build();
            searchLogRepository.save(searchLog);
        } catch (Exception e) {
            // 검색 로그 저장 실패해도 검색은 계속 진행
            log.warn("검색 로그 저장 실패: keyword={}, searchType={}", keyword, searchType, e);
        }
    }

    /**
     * 장소의 해시태그 생성
     */
    private List<String> generateHashtags(Place place) {
        List<String> hashtags = new ArrayList<>();
        
        if (place.getCategory() != null) {
            hashtags.add("#" + place.getCategory().getDisplayName().replace("/", ""));
        }
        
        if (place.getGroup() != null) {
            String groupName = place.getGroup().name();
            if (groupName.equals("관광지")) {
                hashtags.add("#관광지");
            } else if (groupName.equals("맛집")) {
                hashtags.add("#식도락");
            } else if (groupName.equals("카페")) {
                hashtags.add("#감성카페");
            }
        }
        
        return hashtags;
    }

    /**
     * 루트의 해시태그 생성
     */
    private List<String> generateRouteHashtags(Route route) {
        List<String> hashtags = new ArrayList<>();
        
        // 루트 제목에서 키워드 추출 또는 기본 해시태그
        if (route.getTitle() != null) {
            String title = route.getTitle().toLowerCase();
            if (title.contains("빵") || title.contains("베이커리")) {
                hashtags.add("#식도락");
                hashtags.add("#베이커리");
            } else if (title.contains("카페") || title.contains("디저트")) {
                hashtags.add("#식도락");
                hashtags.add("#감성카페");
            } else {
                hashtags.add("#식도락");
            }
        } else {
            hashtags.add("#식도락");
        }
        
        return hashtags;
    }

    /**
     * 루트의 썸네일 이미지 조회
     * 첫 번째 RouteLocation의 name으로 Place를 찾아서 첫 번째 사진을 반환
     */
    private String getRouteThumbnail(Route route) {
        if (route.getLocations().isEmpty()) {
            return null;
        }

        String firstLocationName = route.getLocations().get(0).getName();
        return placeRepository.findByName(firstLocationName)
                .map(place -> {
                    List<PlacePhoto> photos = placePhotoRepository.findByPlaceId(place.getId());
                    return photos.isEmpty() ? null : photos.get(0).getPhotoUrl();
                })
                .orElse(null);
    }
}

