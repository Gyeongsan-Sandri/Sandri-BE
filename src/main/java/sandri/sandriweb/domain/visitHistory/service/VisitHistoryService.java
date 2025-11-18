package sandri.sandriweb.domain.visitHistory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.entity.PlacePhoto;
import sandri.sandriweb.domain.place.repository.PlacePhotoRepository;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.point.enums.ConditionType;
import sandri.sandriweb.domain.point.service.PointService;
import sandri.sandriweb.domain.review.repository.PlaceReviewRepository;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.entity.RouteLocation;
import sandri.sandriweb.domain.route.repository.RouteLocationRepository;
import sandri.sandriweb.domain.route.repository.RouteRepository;
import sandri.sandriweb.domain.visitHistory.dto.TodayRoutePlaceDto;
import sandri.sandriweb.domain.visitHistory.dto.UserVisitHistoryDto;
import sandri.sandriweb.domain.visitHistory.dto.VisitPlaceResponseDto;
import sandri.sandriweb.domain.visitHistory.entity.mapping.UserPlaceHistory;
import sandri.sandriweb.domain.visitHistory.repository.UserPlaceHistoryRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VisitHistoryService {

    private final UserPlaceHistoryRepository userPlaceHistoryRepository;
    private final PlaceRepository placeRepository;
    private final PlacePhotoRepository placePhotoRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PointService pointService;
    private final RouteRepository routeRepository;
    private final RouteLocationRepository routeLocationRepository;
    
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final double VISIT_DISTANCE_THRESHOLD_METERS = 1000.0; // 1km

    /**
     * 장소 방문 확인 및 기록
     * 사용자 GPS 위치와 장소 위치의 거리가 1km 이내인지 확인하고,
     * 조건에 맞으면 방문 기록을 저장합니다.
     *
     * @param user 사용자 엔티티 (Controller에서 전달)
     * @param placeId 장소 ID
     * @param latitude 사용자 GPS 위도
     * @param longitude 사용자 GPS 경도
     * @return 방문 여부 및 방문 기록 ID
     */
    @Transactional
    public VisitPlaceResponseDto checkAndRecordVisit(User user, Long placeId, Double latitude, Double longitude) {
        // 1. 사용자 검증
        if (user == null) {
            throw new RuntimeException("사용자 정보가 없습니다");
        }

        // 2. 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다: placeId=" + placeId));

        if (!place.isEnabled()) {
            throw new RuntimeException("비활성화된 장소입니다: placeId=" + placeId);
        }

        // 3. 좌표 범위 검증
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new RuntimeException("위도는 -90 ~ 90 사이여야 합니다: " + latitude);
        }
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new RuntimeException("경도는 -180 ~ 180 사이여야 합니다: " + longitude);
        }

        // 4. 사용자 GPS 위치를 Point로 변환
        Point userLocation = geometryFactory.createPoint(
                new Coordinate(longitude, latitude)
        );

        // 5. 장소 위치 가져오기
        Point placeLocation = place.getLocation();
        if (placeLocation == null) {
            throw new RuntimeException("장소의 위치 정보가 없습니다: placeId=" + placeId);
        }

        // 6. 거리 계산 (미터 단위)
        double distanceInMeters = calculateDistanceInMeters(userLocation, placeLocation);

        // 7. 1km 이내인지 확인
        if (distanceInMeters <= VISIT_DISTANCE_THRESHOLD_METERS) {
            // 방문 기록 저장
            UserPlaceHistory visitHistory = UserPlaceHistory.builder()
                    .user(user)
                    .place(place)
                    .build();

            UserPlaceHistory savedHistory = userPlaceHistoryRepository.save(visitHistory);
            log.info("장소 방문 기록 저장 완료: userId={}, placeId={}, distance={}m",
                    user.getId(), placeId, distanceInMeters);

            // 8. 포인트 적립 처리
            try {
                pointService.earnPoints(user, ConditionType.PLACE_VISIT);
            } catch (Exception e) {
                log.error("포인트 적립 중 오류 발생: userId={}, placeId={}, error={}",
                        user.getId(), placeId, e.getMessage(), e);
                // 포인트 적립 실패해도 방문 기록은 유지
            }

            return VisitPlaceResponseDto.builder()
                    .visited(true)
                    .visitHistoryId(savedHistory.getId())
                    .build();
        } else {
            log.info("장소 방문 조건 미충족: userId={}, placeId={}, distance={}m (임계값: {}m)",
                    user.getId(), placeId, distanceInMeters, VISIT_DISTANCE_THRESHOLD_METERS);

            return VisitPlaceResponseDto.builder()
                    .visited(false)
                    .visitHistoryId(null)
                    .build();
        }
    }

    /**
     * 현재 로그인한 사용자의 모든 방문 기록 조회 (DTO 리스트 반환)
     * N+1 문제 해결: Fetch Join과 일괄 조회 사용
     * @param user 사용자 엔티티 (Controller에서 전달)
     * @return 해당 사용자의 모든 방문 기록 DTO 리스트
     */
    public List<UserVisitHistoryDto> getUserVisitHistory(User user) {
        // 사용자 검증
        if (user == null) {
            throw new RuntimeException("사용자 정보가 없습니다");
        }

        log.info("사용자 방문 기록 조회: userId={}", user.getId());

        // 사용자의 모든 방문 기록 조회 (Place, PlacePhoto를 Fetch Join으로 함께 조회)
        List<UserPlaceHistory> histories = userPlaceHistoryRepository.findByUserIdWithPlaceAndPhotos(user.getId());

        // 장소 ID 목록 추출
        Set<Long> placeIds = histories.stream()
                .map(h -> h.getPlace().getId())
                .collect(Collectors.toSet());

        // 리뷰가 있는 장소 ID 일괄 조회 (N+1 문제 해결)
        Set<Long> placeIdsWithReview = placeReviewRepository
                .findPlaceIdsWithReviewByUserIdAndPlaceIds(user.getId(), placeIds);

        // DTO 리스트로 변환
        return histories.stream()
                .map(history -> {
                    Place place = history.getPlace();

                    // 첫 번째 사진 URL 가져오기 (order가 0인 사진)
                    String firstPhotoUrl = place.getPhotos().stream()
                            .filter(photo -> photo.getOrder() == 0)
                            .findFirst()
                            .map(PlacePhoto::getPhotoUrl)
                            .orElse(null);

                    // 리뷰 작성 여부 확인 (일괄 조회한 Set에서 확인)
                    boolean hasReview = placeIdsWithReview.contains(place.getId());

                    // 요일 계산
                    String dayOfWeek = UserVisitHistoryDto.getDayOfWeekInKorean(
                            history.getCreatedAt().getDayOfWeek()
                    );

                    return UserVisitHistoryDto.builder()
                            .visitHistoryId(history.getId())
                            .placeId(place.getId())
                            .placeName(place.getName())
                            .firstPhotoUrl(firstPhotoUrl)
                            .visitedAt(history.getCreatedAt().toLocalDate())
                            .dayOfWeek(dayOfWeek)
                            .hasReview(hasReview)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 두 지점 간의 거리 계산 (Haversine 공식, 미터 단위)
     * @param point1 첫 번째 지점
     * @param point2 두 번째 지점
     * @return 거리 (미터)
     */
    private double calculateDistanceInMeters(Point point1, Point point2) {
        if (point1 == null || point2 == null) {
            throw new IllegalArgumentException("지점 정보가 null일 수 없습니다");
        }

        double lat1 = point1.getY();
        double lon1 = point1.getX();
        double lat2 = point2.getY();
        double lon2 = point2.getX();

        // 지구 반지름 (미터)
        final double R = 6371000.0;

        // 위도, 경도를 라디안으로 변환
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Haversine 공식
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 거리 계산 (미터)
        return R * c;
    }

    /**
     * 오늘 날짜에 해당하는 루트의 장소 목록 조회
     * 로그인한 사용자가 참여한 루트 중 오늘 날짜에 해당하는 장소들을 조회합니다.
     * N+1 문제 해결: Place 일괄 조회 및 사진 URL 배치 조회 사용
     * 
     * @param user 사용자 엔티티 (Controller에서 전달)
     * @return 오늘 날짜에 해당하는 장소 목록 (장소 DTO, 총 장소 개수, 방문 순서 포함)
     */
    public List<TodayRoutePlaceDto> getTodayRoutePlaces(User user) {
        // 사용자 검증
        if (user == null) {
            throw new RuntimeException("사용자 정보가 없습니다");
        }

        LocalDate today = LocalDate.now();
        log.info("오늘 일정 장소 조회: userId={}, today={}", user.getId(), today);

        // 1. 사용자가 참여한 루트 중 오늘 날짜에 해당하는 루트 조회
        List<Route> todayRoutes = routeRepository.findTodayRoutesByUserId(user.getId(), today);

        if (todayRoutes.isEmpty()) {
            log.info("오늘 날짜에 해당하는 루트가 없습니다: userId={}", user.getId());
            return List.of();
        }

        // 2. 모든 루트의 오늘 날짜 장소 목록 수집
        List<RouteLocation> allTodayLocations = new java.util.ArrayList<>();
        Map<RouteLocation, Integer> locationToTotalCountMap = new HashMap<>();
        
        for (Route route : todayRoutes) {
            // 오늘 날짜가 루트의 몇 번째 날인지 계산 (1부터 시작)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(route.getStartDate(), today);
            int todayDayNumber = (int) daysBetween + 1;

            // 해당 dayNumber의 장소 목록 조회
            List<RouteLocation> todayLocations = routeLocationRepository
                    .findByRouteAndDayNumberOrderByDisplayOrderAsc(route, todayDayNumber);

            if (!todayLocations.isEmpty()) {
                // 루트의 총 장소 개수
                int totalPlaceCount = route.getLocations().size();
                
                // 각 장소에 총 개수 매핑
                for (RouteLocation location : todayLocations) {
                    locationToTotalCountMap.put(location, totalPlaceCount);
                }
                
                allTodayLocations.addAll(todayLocations);
            }
        }

        if (allTodayLocations.isEmpty()) {
            return List.of();
        }

        // 3. 모든 RouteLocation의 name을 수집하여 Place 일괄 조회 (N+1 문제 해결)
        Set<String> placeNames = allTodayLocations.stream()
                .map(RouteLocation::getName)
                .collect(Collectors.toSet());

        // Place 일괄 조회 (N+1 문제 해결)
        List<Place> places = placeRepository.findByNameIn(placeNames);
        
        // Place 이름으로 매핑 (name -> Place)
        Map<String, Place> placeByNameMap = places.stream()
                .collect(Collectors.toMap(Place::getName, place -> place, (existing, replacement) -> existing));

        // 4. Place ID 목록 추출하여 사진 URL 배치 조회 (N+1 문제 해결)
        List<Long> placeIds = placeByNameMap.values().stream()
                .map(Place::getId)
                .collect(Collectors.toList());

        Map<Long, String> photoUrlByPlaceId = getPhotoUrlByPlaceIds(placeIds);

        // 5. Place 이름으로 사진 URL 매핑 (name -> photoUrl)
        Map<String, String> photoUrlByNameMap = placeByNameMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> photoUrlByPlaceId.getOrDefault(entry.getValue().getId(), null)
                ));

        // 6. DTO 변환
        return allTodayLocations.stream()
                .map(location -> {
                    String placeName = location.getName();
                    String thumbnail = photoUrlByNameMap.getOrDefault(placeName, null);

                    // 장소 정보 생성
                    TodayRoutePlaceDto.PlaceInfo placeInfo = TodayRoutePlaceDto.PlaceInfo.builder()
                            .thumbnail(thumbnail)
                            .placeName(placeName)
                            .address(location.getAddress())
                            .build();

                    return TodayRoutePlaceDto.builder()
                            .placeInfo(placeInfo)
                            .totalPlaceCount(locationToTotalCountMap.get(location))
                            .visitOrder(location.getDisplayOrder())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 여러 장소의 첫 번째 사진 URL을 조회하여 Place ID별로 매핑
     * N+1 문제 방지를 위해 배치 조회 사용 (PlaceService 패턴 참고)
     * @param placeIds 장소 ID 목록
     * @return Place ID를 키로, 사진 URL을 값으로 하는 Map
     */
    private Map<Long, String> getPhotoUrlByPlaceIds(List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return new HashMap<>();
        }

        // 배치 조회로 각 장소당 첫 번째 사진만 조회 (N+1 문제 방지)
        List<Object[]> photoResults = placePhotoRepository.findFirstPhotoUrlByPlaceIdIn(placeIds);

        return photoResults.stream()
                .collect(Collectors.toMap(
                        result -> ((Number) result[0]).longValue(), // place_id
                        result -> (String) result[1],  // photo_url
                        (existing, replacement) -> existing // 중복 시 기존 값 유지
                ));
    }
}
