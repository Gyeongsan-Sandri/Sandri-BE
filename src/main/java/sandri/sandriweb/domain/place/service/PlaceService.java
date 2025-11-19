package sandri.sandriweb.domain.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sandri.sandriweb.domain.admin.dto.CreatePlacePhotoRequestDto;
import sandri.sandriweb.domain.place.dto.*;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.entity.PlacePhoto;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.enums.PlaceCategory;
import sandri.sandriweb.domain.place.entity.mapping.UserPlace;
import sandri.sandriweb.domain.place.repository.PlacePhotoRepository;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.place.repository.UserPlaceRepository;
import sandri.sandriweb.domain.review.service.ReviewService;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.global.service.GoogleGeocodingService;
import sandri.sandriweb.global.service.GooglePlacesService;
import sandri.sandriweb.global.service.S3Service;
import sandri.sandriweb.global.service.dto.GeocodingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlacePhotoRepository placePhotoRepository;
    private final ReviewService reviewService;
    private final UserPlaceRepository userPlaceRepository;
    private final UserRepository userRepository;
    private final GoogleGeocodingService googleGeocodingService;
    private final GooglePlacesService googlePlacesService;
    private final S3Service s3Service;
    
    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;
    
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /*
     * 관광지 상세 정보 조회 (기본 정보만, 리뷰 제외)
     * @param placeId 관광지 ID
     * @return PlaceDetailResponseDto (리뷰 정보 제외)
     */
    public PlaceDetailResponseDto getPlaceDetail(Long placeId) {
        // 1. 관광지 기본 정보 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));

        // 2. 공식 사진 조회 (enabled된 것만, order 순서대로 정렬 후 DTO 고유의 order 부여)
        List<PlacePhoto> enabledPhotos = placePhotoRepository.findByPlaceId(placeId);
        List<PlaceDetailResponseDto.PhotoDto> officialPhotos = new ArrayList<>();
        int dtoOrder = 0;
        for (PlacePhoto photo : enabledPhotos) {
            officialPhotos.add(PlaceDetailResponseDto.PhotoDto.builder()
                    .order(dtoOrder++) // DTO 고유의 order (0부터 시작)
                    .photoUrl(photo.getPhotoUrl())
                    .build());
        }

        // 3. 평점 계산
        Double averageRating = reviewService.getAverageRating(placeId);

        // 4. DTO 생성 및 반환 (리뷰 정보 및 근처 장소는 별도 API로 조회)
        return PlaceDetailResponseDto.builder()
                .placeId(place.getId())
                .name(place.getName())
                .address(place.getAddress())
                .groupName(place.getGroup() != null ? place.getGroup().name() : null)
                .categoryName(place.getCategory() != null ? place.getCategory().getDisplayName() : null)
                .rating(averageRating)
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .summary(place.getSummery())
                .information(place.getInformation())
                .officialPhotos(officialPhotos)
                .build();
    }

    /**
     * Place ID로 근처 가볼만한 곳 조회 (대분류별, 좋아요 많은 순)
     * @param placeId 기준 관광지 ID
     * @param groupName 대분류 이름 (관광지/맛집/카페)
     * @param limit 조회할 개수
     * @return 근처 관광지 리스트 (좋아요 많은 순)
     */
    public List<NearbyPlaceDto> getNearbyPlacesByGroup(Long placeId, String groupName, int limit) {
        // 1. 대분류 검증
        try {
            PlaceCategory.valueOf(groupName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("대분류는 '관광지', '맛집', '카페' 중 하나여야 합니다.");
        }
        
        // 2. 기준 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));
        
        // 3. 10km 반경 (미터 단위)
        double radius = 10000.0;
        
        // 4. 대분류별 근처 장소 조회 (좋아요 많은 순)
        List<Place> nearbyPlaces = placeRepository.findNearbyPlacesByGroupOrderByLikeCount(
                place.getLocation(),
                radius,
                place.getId(),
                groupName,
                limit
        );
        
        if (nearbyPlaces.isEmpty()) {
            return List.of();
        }
        
        // 5. 사진 조회 및 매핑 (공통 헬퍼 메서드 사용)
        List<Long> nearbyPlaceIds = nearbyPlaces.stream()
                .map(Place::getId)
                .collect(Collectors.toList());
        Map<Long, String> photoUrlByPlaceId = getPhotoUrlByPlaceIds(nearbyPlaceIds);
        
        // 6. 기준 장소의 위치
        Point centerLocation = place.getLocation();
        
        // 7. DTO 변환 (좋아요 순위 포함)
        return java.util.stream.IntStream.range(0, nearbyPlaces.size())
                .mapToObj(index -> {
                    Place nearbyPlace = nearbyPlaces.get(index);
                    String thumbnailUrl = photoUrlByPlaceId.get(nearbyPlace.getId());
                    Long distance = calculateDistanceInMeters(centerLocation, nearbyPlace.getLocation());
                    
                    // 좋아요 순위 (1부터 시작, 1이 가장 좋아요가 많음)
                    int rank = index + 1;
                    
                    return NearbyPlaceDto.builder()
                            .placeId(nearbyPlace.getId())
                            .name(nearbyPlace.getName())
                            .thumbnailUrl(thumbnailUrl)
                            .distanceInMeters(distance)
                            .rank(rank)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Place ID로 근처 가볼만한 곳 조회 (카테고리 필터 없음, 반경 제한 없음)
     * 전체 Place 목록에서 현재 관광지와 위치상 가까운 순으로 조회 (현재 장소 포함)
     * @param placeId 기준 관광지 ID
     * @param limit 조회할 개수
     * @return 근처 관광지 리스트 (가까운 순으로 정렬, 현재 장소 포함, rank 포함)
     */
    public List<NearbyPlaceDto> getNearbyPlaces(Long placeId, int limit) {
        // 1. 기준 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));
        
        // 2. 위치 정보 확인
        if (place.getLocation() == null) {
            throw new RuntimeException("기준 장소의 위치 정보가 없습니다.");
        }
        
        // 3. 근처 장소 조회 (반경 제한 없음, 가까운 순으로 정렬, 현재 장소 포함)
        // limit + 1개 조회하여 현재 장소가 포함되어 있는지 확인
        List<Place> nearbyPlaces = placeRepository.findNearestPlaces(
                place.getLocation(),
                limit + 1
        );
        
        if (nearbyPlaces.isEmpty()) {
            return List.of();
        }
        
        // 4. 현재 장소가 포함되어 있는지 확인하고, 없으면 추가
        boolean containsCurrentPlace = nearbyPlaces.stream()
                .anyMatch(p -> p.getId().equals(placeId));
        
        if (!containsCurrentPlace) {
            // 현재 장소를 첫 번째로 추가 (거리 0이므로)
            nearbyPlaces.add(0, place);
            // limit 개수만큼만 유지
            if (nearbyPlaces.size() > limit) {
                nearbyPlaces = nearbyPlaces.subList(0, limit);
            }
        } else {
            // 현재 장소가 포함되어 있으면 limit 개수만큼만 유지
            if (nearbyPlaces.size() > limit) {
                nearbyPlaces = nearbyPlaces.subList(0, limit);
            }
        }
        
        // 5. 사진 조회 및 매핑 (공통 헬퍼 메서드 사용)
        List<Long> nearbyPlaceIds = nearbyPlaces.stream()
                .map(Place::getId)
                .collect(Collectors.toList());
        Map<Long, String> photoUrlByPlaceId = getPhotoUrlByPlaceIds(nearbyPlaceIds);
        
        // 6. 기준 장소의 위치
        Point centerLocation = place.getLocation();
        
        // 7. DTO 변환 (rank 포함)
        final Point finalCenterLocation = centerLocation;
        final Long currentPlaceId = placeId;
        
        // Place ID와 DTO를 매핑하기 위한 Map 생성 (rank 계산용)
        Map<String, Long> nameToPlaceIdMap = nearbyPlaces.stream()
                .collect(Collectors.toMap(
                        Place::getName,
                        Place::getId,
                        (existing, replacement) -> existing
                ));
        
        List<NearbyPlaceDto> dtos = nearbyPlaces.stream()
                .map(nearbyPlace -> {
                    String thumbnailUrl = photoUrlByPlaceId.get(nearbyPlace.getId());
                    Long distance = calculateDistanceInMeters(finalCenterLocation, nearbyPlace.getLocation());
                    
                    return NearbyPlaceDto.builder()
                            .placeId(nearbyPlace.getId())
                            .name(nearbyPlace.getName())
                            .thumbnailUrl(thumbnailUrl)
                            .latitude(nearbyPlace.getLatitude())
                            .longitude(nearbyPlace.getLongitude())
                            .distanceInMeters(distance)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 거리순으로 정렬 (거리가 같으면 이름순)
        dtos.sort((a, b) -> {
            // 위도/경도로 거리 계산하여 정렬
            Long distanceA = calculateDistanceInMeters(
                    finalCenterLocation,
                    geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(
                            a.getLongitude() != null ? a.getLongitude() : 0,
                            a.getLatitude() != null ? a.getLatitude() : 0
                    ))
            );
            Long distanceB = calculateDistanceInMeters(
                    finalCenterLocation,
                    geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(
                            b.getLongitude() != null ? b.getLongitude() : 0,
                            b.getLatitude() != null ? b.getLatitude() : 0
                    ))
            );
            
            int distanceCompare = Long.compare(
                    distanceA != null ? distanceA : Long.MAX_VALUE,
                    distanceB != null ? distanceB : Long.MAX_VALUE
            );
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return a.getName().compareTo(b.getName());
        });
        
        // rank 부여 (현재 장소는 0, 나머지는 1부터 시작)
        // 현재 장소의 인덱스 찾기
        int currentPlaceIndex = java.util.stream.IntStream.range(0, dtos.size())
                .filter(index -> {
                    NearbyPlaceDto d = dtos.get(index);
                    Long pid = nameToPlaceIdMap.get(d.getName());
                    return pid != null && pid.equals(currentPlaceId);
                })
                .findFirst()
                .orElse(-1);
        
        return java.util.stream.IntStream.range(0, dtos.size())
                .mapToObj(index -> {
                    NearbyPlaceDto dto = dtos.get(index);
                    // 현재 장소인지 확인 (placeId로 확인)
                    Long dtoPlaceId = nameToPlaceIdMap.get(dto.getName());
                    boolean isCurrentPlace = dtoPlaceId != null && dtoPlaceId.equals(currentPlaceId);
                    
                    // rank 계산: 현재 장소는 0, 나머지는 현재 장소를 제외한 순서로 1부터 시작
                    int rank;
                    if (isCurrentPlace) {
                        rank = 0;
                    } else if (index < currentPlaceIndex) {
                        // 현재 장소보다 앞에 있으면: index + 1
                        rank = index + 1;
                    } else {
                        // 현재 장소보다 뒤에 있으면: index (현재 장소를 제외)
                        rank = index;
                    }
                    
                    return NearbyPlaceDto.builder()
                            .placeId(dto.getPlaceId())
                            .name(dto.getName())
                            .thumbnailUrl(dto.getThumbnailUrl())
                            .latitude(dto.getLatitude())
                            .longitude(dto.getLongitude())
                            .distanceInMeters(dto.getDistanceInMeters())
                            .rank(rank)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 여러 장소의 첫 번째 사진 URL을 조회하여 Place ID별로 매핑
     * N+1 문제 방지를 위해 배치 조회 사용
     * @param placeIds 장소 ID 목록
     * @return Place ID를 키로, 사진 URL을 값으로 하는 Map
     */
    private Map<Long, String> getPhotoUrlByPlaceIds(List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        // 배치 조회로 각 장소당 첫 번째 사진만 조회 (N+1 문제 방지)
        List<Object[]> photoResults = placePhotoRepository.findFirstPhotoUrlByPlaceIdIn(placeIds);
        
        return photoResults.stream()
                .collect(Collectors.toMap(
                        result -> ((Number) result[0]).longValue(), // place_id
                        result -> (String) result[1]  // photo_url
                ));
    }
    
    /**
     * 전체 장소 목록 조회 (ID와 이름만)
     * @return 장소 목록 (ID, 이름)
     */
    @Transactional(readOnly = true)
    public List<PlaceListDto> getAllPlaces() {
        List<Place> places = placeRepository.findAll();
        
        return places.stream()
                .filter(Place::isEnabled) // enabled된 것만
                .map(place -> PlaceListDto.builder()
                        .placeId(place.getId())
                        .name(place.getName())
                        .build())
                .collect(Collectors.toList());
    }
    
    /*
     * 두 지점 간의 거리 계산 (Haversine 공식, 미터 단위)
     * @param point1 첫 번째 지점
     * @param point2 두 번째 지점
     * @return 거리 (미터)
     */
    private Long calculateDistanceInMeters(Point point1, Point point2) {
        if (point1 == null || point2 == null) {
            return null;
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
        double distance = R * c;
        
        return Math.round(distance);
    }

    /**
     * 카테고리별 장소 조회 (좋아요 많은 순, 커서 기반 페이징)
     * @param categoryDisplayName 카테고리 표시 이름 ('자연/힐링', '역사/전통', '문화/체험', '식도락')
     * @param count 조회할 개수
     * @param lastPlaceId 마지막 장소 ID (더보기용, null이면 처음부터)
     * @param userId 사용자 ID (로그인한 경우에만 제공, null 가능)
     * @return 카테고리별 장소 리스트
     */
    @Transactional(readOnly = true)
    public List<SimplePlaceDto> getPlacesByCategory(String categoryDisplayName, int count, Long lastPlaceId, Long userId) {
        // 카테고리 표시 이름을 enum으로 변환
        Category category = convertDisplayNameToCategory(categoryDisplayName);
        if (category == null) {
            throw new RuntimeException("유효하지 않은 카테고리입니다: " + categoryDisplayName);
        }

        // 카테고리별 장소 조회 (좋아요 많은 순, 커서 기반 페이징)
        List<Place> places;
        if (lastPlaceId == null) {
            // 첫 페이지 조회
            places = placeRepository.findByCategoryOrderByLikeCountDesc(category.name(), count);
        } else {
            // 커서 기반 페이징: 마지막 장소의 정보를 조회
            Place lastPlace = placeRepository.findById(lastPlaceId)
                    .orElseThrow(() -> new RuntimeException("마지막 장소를 찾을 수 없습니다."));
            
            long lastLikeCount = placeRepository.getLikeCountByPlaceId(lastPlaceId);
            
            places = placeRepository.findByCategoryOrderByLikeCountDescWithCursor(
                    category.name(),
                    lastLikeCount,
                    lastPlace.getCreatedAt(),
                    lastPlaceId,
                    count
            );
        }

        if (places.isEmpty()) {
            return List.of();
        }

        // 장소 ID 리스트
        List<Long> placeIds = places.stream()
                .map(Place::getId)
                .collect(Collectors.toList());

        // 사진 조회 및 매핑 (공통 헬퍼 메서드 사용)
        Map<Long, String> photoUrlByPlaceId = getPhotoUrlByPlaceIds(placeIds);

        // 사용자가 좋아요한 장소 ID 조회 (로그인한 경우)
        Map<Long, Boolean> likedPlaceIds;
        if (userId != null) {
            List<Long> likedIds = userPlaceRepository.findLikedPlaceIdsByUserId(userId, placeIds);
            likedPlaceIds = likedIds.stream()
                    .collect(Collectors.toMap(
                            placeId -> placeId,
                            placeId -> true
                    ));
        } else {
            likedPlaceIds = new java.util.HashMap<>();
        }

        // DTO 변환
        return places.stream()
                .map(place -> {
                    // 사진 URL 추출 (각 장소당 한 장씩)
                    String thumbnailUrl = photoUrlByPlaceId.get(place.getId());

                    // 사용자가 좋아요한 장소인지 확인
                    Boolean isLiked = userId != null ? likedPlaceIds.getOrDefault(place.getId(), false) : null;

                    return SimplePlaceDto.builder()
                            .placeId(place.getId())
                            .name(place.getName())
                            .address(place.getAddress())
                            .thumbnailUrl(thumbnailUrl)
                            .isLiked(isLiked)
                            .groupName(place.getGroup() != null ? place.getGroup().name() : null)
                            .categoryName(place.getCategory() != null ? place.getCategory().getDisplayName() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 장소 이름으로 Place를 찾거나 생성하고 장소 모아보기에 추가
     * @param placeName 장소 이름
     * @param userId 사용자 ID
     * @return 추가된 Place ID
     */
    @Transactional
    public Long addPlaceToCollectionByName(String placeName, Long userId) {
        // Place 찾기 또는 생성
        Place place = findOrCreatePlaceByName(placeName);
        
        // 장소 모아보기에 추가 (좋아요)
        addToCollection(place.getId(), userId);
        
        return place.getId();
    }

    /**
     * 장소를 장소 모아보기에 추가 (좋아요)
     * @param placeId 장소 ID
     * @param userId 사용자 ID
     */
    @Transactional
    public void addToCollection(Long placeId, Long userId) {
        // 이미 좋아요가 있는지 확인
        java.util.Optional<UserPlace> existingUserPlace = userPlaceRepository.findByUserIdAndPlaceId(userId, placeId);
        
        if (existingUserPlace.isPresent()) {
            UserPlace userPlace = existingUserPlace.get();
            if (userPlace.isEnabled()) {
                log.info("이미 장소 모아보기에 추가되어 있음: placeId={}, userId={}", placeId, userId);
                return; // 이미 추가되어 있음
            } else {
                // 비활성화된 경우 재활성화
                userPlace.enable();
                userPlaceRepository.save(userPlace);
                log.info("장소 모아보기 재활성화: placeId={}, userId={}", placeId, userId);
                return;
            }
        }
        
        // 새로 추가
        User user = userRepository.getReferenceById(userId);
        Place place = placeRepository.getReferenceById(placeId);
        UserPlace newUserPlace = UserPlace.builder()
                .user(user)
                .place(place)
                .enabled(true)  // 명시적으로 enabled 설정
                .build();
        userPlaceRepository.save(newUserPlace);
        log.info("장소 모아보기에 추가 완료: placeId={}, userId={}", placeId, userId);
    }

    /**
     * 장소 좋아요 토글
     * @param placeId 장소 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태 (true: 좋아요 활성화, false: 좋아요 비활성화)
     */
    @Transactional
    public boolean toggleLike(Long placeId, Long userId) {
        // 장소 존재 확인 (exists로 최적화)
        if (!placeRepository.existsById(placeId)) {
            throw new RuntimeException("장소를 찾을 수 없습니다.");
        }
        
        // 사용자 존재 확인 (exists로 최적화)
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        // 기존 좋아요 조회
        return userPlaceRepository.findByUserIdAndPlaceId(userId, placeId)
                .map(userPlace -> {
                    // 이미 좋아요가 있는 경우: 토글
                    if (userPlace.isEnabled()) {
                        userPlace.disable(); // 좋아요 취소
                        userPlaceRepository.save(userPlace);
                        return false;
                    } else {
                        userPlace.enable(); // 좋아요 재활성화
                        userPlaceRepository.save(userPlace);
                        return true;
                    }
                })
                .orElseGet(() -> {
                    // 좋아요가 없는 경우: 새로 생성 (User와 Place는 프록시로 로드)
                    User user = userRepository.getReferenceById(userId);
                    Place place = placeRepository.getReferenceById(placeId);
                    UserPlace newUserPlace = UserPlace.builder()
                            .user(user)
                            .place(place)
                            .build();
                    userPlaceRepository.save(newUserPlace);
                    return true;
                });
    }

    /**
     * 장소 이름으로 Place 찾기 또는 생성 (DB 우선, 없으면 Google Places 검색)
     * @param placeName 장소 이름
     * @return 찾거나 생성된 Place
     */
    @Transactional
    public Place findOrCreatePlaceByName(String placeName) {
        if (placeName == null || placeName.trim().isEmpty()) {
            throw new RuntimeException("장소 이름은 필수입니다");
        }

        String trimmedName = placeName.trim();
        
        // 1단계: DB에서 먼저 검색
        java.util.Optional<Place> existingPlace = placeRepository.findByName(trimmedName);
        if (existingPlace.isPresent()) {
            log.info("DB에서 장소 찾음: placeId={}, name={}", existingPlace.get().getId(), trimmedName);
            return existingPlace.get();
        }

        // 2단계: DB에 없으면 Google Places API로 검색
        log.info("DB에 장소 없음. Google Places API로 검색: name={}", trimmedName);
        List<GooglePlacesService.PlaceSearchResult> googleResults = googlePlacesService.searchPlaces(trimmedName, "ko", "kr");
        
        if (googleResults == null || googleResults.isEmpty()) {
            throw new RuntimeException("장소를 찾을 수 없습니다. DB와 Google Maps 모두에서 검색했지만 결과가 없습니다: " + trimmedName);
        }

        // 첫 번째 결과 사용
        GooglePlacesService.PlaceSearchResult googleResult = googleResults.get(0);
        
        // Google Places 정보로 Place 생성
        PlaceCategory group = extractGroupFromTypes(googleResult.getTypes());
        Category category = extractCategoryFromTypes(googleResult.getTypes());

        // 좌표 생성
        if (googleResult.getLatitude() == null || googleResult.getLongitude() == null) {
            throw new RuntimeException("장소의 위치 정보를 찾을 수 없습니다: " + trimmedName);
        }

        Coordinate coordinate = new Coordinate(
                googleResult.getLongitude(),
                googleResult.getLatitude()
        );
        Point location = geometryFactory.createPoint(coordinate);

        // Place 생성
        Place place = Place.builder()
                .name(trimmedName)
                .address(googleResult.getAddress())
                .location(location)
                .summery(null)
                .information(null)
                .group(group)
                .category(category)
                .build();

        Place savedPlace = placeRepository.save(place);
        log.info("Google Places 정보로 장소 생성 완료: placeId={}, name={}", savedPlace.getId(), trimmedName);

        // 사진이 있으면 PlacePhoto 추가
        if (googleResult.getPhotoReference() != null) {
            String photoUrl = googleResult.getPhotoUrl(googleMapsApiKey, 800);
            if (photoUrl != null) {
                PlacePhoto placePhoto = PlacePhoto.builder()
                        .place(savedPlace)
                        .photoUrl(photoUrl)
                        .order(0)
                        .build();
                placePhotoRepository.save(placePhoto);
                log.info("Google Places 사진 추가: placeId={}, photoUrl={}", savedPlace.getId(), photoUrl);
            }
        }

        return savedPlace;
    }

    /**
     * Google Places types에서 대분류 추출
     */
    private PlaceCategory extractGroupFromTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return PlaceCategory.관광지; // 기본값
        }

        for (String type : types) {
            if (type.contains("restaurant") || type.contains("food") || type.contains("meal")) {
                return PlaceCategory.맛집;
            } else if (type.contains("cafe") || type.contains("coffee")) {
                return PlaceCategory.카페;
            } else if (type.contains("tourist") || type.contains("museum") || type.contains("park") || 
                       type.contains("attraction") || type.contains("point_of_interest")) {
                return PlaceCategory.관광지;
            }
        }
        return PlaceCategory.관광지; // 기본값
    }

    /**
     * Google Places types에서 세부 카테고리 추출
     */
    private Category extractCategoryFromTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Category.자연_힐링; // 기본값
        }

        for (String type : types) {
            if (type.contains("restaurant") || type.contains("food") || type.contains("meal") || 
                type.contains("cafe") || type.contains("coffee")) {
                return Category.식도락;
            } else if (type.contains("natural") || type.contains("park") || type.contains("hiking") || 
                       type.contains("beach") || type.contains("mountain")) {
                return Category.자연_힐링;
            } else if (type.contains("museum") || type.contains("art") || type.contains("gallery") || 
                       type.contains("theater") || type.contains("stadium")) {
                return Category.문화_체험;
            } else if (type.contains("temple") || type.contains("church") || type.contains("shrine") || 
                       type.contains("historical") || type.contains("monument")) {
                return Category.역사_전통;
            }
        }
        return Category.자연_힐링; // 기본값
    }

    /**
     * 장소 생성 (관리자용)
     * @param request 장소 생성 요청 DTO
     * @return 생성된 장소 ID
     */
    @Transactional
    public Long createPlace(CreatePlaceRequestDto request, List<MultipartFile> photoFiles) {
        // 중복 검사
        if (placeRepository.existsByName(request.getName())) {
            throw new RuntimeException("이미 존재하는 장소 이름입니다: " + request.getName());
        }

        Coordinate coordinate = resolveCoordinate(request);
        double latitude = coordinate.getY();
        double longitude = coordinate.getX();
        validateCoordinateRange(latitude, longitude);

        String resolvedAddress = StringUtils.hasText(request.getAddress())
                ? request.getAddress()
                : null;

        // 같은 location을 가진 enabled된 Place가 있으면 disable 처리 (거리 0m 이내)
        Point location = geometryFactory.createPoint(coordinate);
        List<Place> nearbyPlaces = placeRepository.findNearbyPlaces(location, 0.0, -1L, 1);
        if (!nearbyPlaces.isEmpty()) {
            Place existingPlace = nearbyPlaces.get(0);
            existingPlace.disable();
            placeRepository.save(existingPlace);
            log.info("같은 location을 가진 기존 장소 비활성화: placeId={}, name={}", 
                    existingPlace.getId(), existingPlace.getName());
        }

        // Place 생성
        Place place = Place.builder()
                .name(request.getName())
                .address(resolvedAddress)
                .location(location)
                .summery(request.getSummary())
                .information(request.getInformation())
                .group(request.getGroup())
                .category(request.getCategory())
                .build();

        Place savedPlace = placeRepository.save(place);
        log.info("장소 생성 완료: placeId={}, name={}", savedPlace.getId(), savedPlace.getName());

        attachUploadedPhotos(savedPlace, photoFiles);

        return savedPlace.getId();
    }

    private Coordinate resolveCoordinate(CreatePlaceRequestDto request) {
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;

        if (hasLatitude && hasLongitude) {
            return new Coordinate(longitude, latitude);
        }

        if (hasLatitude ^ hasLongitude) {
            throw new RuntimeException("위도와 경도는 함께 입력해야 합니다.");
        }

        if (!StringUtils.hasText(request.getAddress())) {
            throw new RuntimeException("주소 또는 위도/경도 중 하나는 반드시 입력해야 합니다.");
        }

        GeocodingResult geocodingResult = googleGeocodingService.geocode(request.getAddress())
                .orElseThrow(() -> new RuntimeException("입력한 주소로 좌표를 찾을 수 없습니다. 주소를 다시 확인해주세요."));

        request.setLatitude(geocodingResult.getLatitude());
        request.setLongitude(geocodingResult.getLongitude());
        if (!StringUtils.hasText(request.getAddress()) && StringUtils.hasText(geocodingResult.getFormattedAddress())) {
            request.setAddress(geocodingResult.getFormattedAddress());
        }

        log.info("주소를 통한 좌표 변환 성공: address={}, lat={}, lng={}", request.getAddress(),
                geocodingResult.getLatitude(), geocodingResult.getLongitude());

        return new Coordinate(geocodingResult.getLongitude(), geocodingResult.getLatitude());
    }

    private void validateCoordinateRange(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new RuntimeException("위도는 -90 ~ 90 사이여야 합니다: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new RuntimeException("경도는 -180 ~ 180 사이여야 합니다: " + longitude);
        }
    }

    private void attachUploadedPhotos(Place place, List<MultipartFile> photoFiles) {
        if (CollectionUtils.isEmpty(photoFiles)) {
            return;
        }

        List<MultipartFile> validFiles = photoFiles.stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());

        if (validFiles.isEmpty()) {
            return;
        }

        List<String> uploadedUrls = s3Service.uploadFiles(validFiles);
        List<PlacePhoto> placePhotos = new ArrayList<>();
        for (int i = 0; i < uploadedUrls.size(); i++) {
            placePhotos.add(PlacePhoto.builder()
                    .place(place)
                    .photoUrl(uploadedUrls.get(i))
                    .order(i)
                    .enabled(true)
                    .build());
        }

        if (!placePhotos.isEmpty()) {
            placePhotoRepository.saveAll(placePhotos);
            log.info("장소 사진 업로드 완료: placeId={}, photoCount={}", place.getId(), placePhotos.size());
        }
    }

    /**
     * 장소 정보 수정 (관리자용)
     * @param placeId 장소 ID
     * @param request 수정 요청 DTO
     * @return 수정된 장소 ID
     */
    @Transactional
    public Long updatePlace(Long placeId, UpdatePlaceRequestDto request) {
        // 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        // 이름 변경 시 중복 검사 (다른 장소와 이름이 겹치는지 확인)
        if (request.getName() != null && !request.getName().equals(place.getName())) {
            if (placeRepository.existsByName(request.getName())) {
                throw new RuntimeException("이미 존재하는 장소 이름입니다: " + request.getName());
            }
        }

        // 위치 정보 업데이트 (위도/경도가 모두 제공된 경우)
        Point location = null;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            location = geometryFactory.createPoint(
                    new org.locationtech.jts.geom.Coordinate(request.getLongitude(), request.getLatitude())
            );
        }

        // 부분 업데이트 (null이 아닌 필드만 업데이트)
        place.update(
                request.getName(),
                request.getAddress(),
                location,
                request.getSummary(),
                request.getInformation(),
                request.getGroup(),
                request.getCategory()
        );

        Place savedPlace = placeRepository.save(place);

        // 사진 업데이트 (요청에 photos가 포함된 경우)
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            // 기존 enabled된 사진 조회
            List<PlacePhoto> existingPhotos = placePhotoRepository.findByPlaceId(placeId);
            
            // 기존 사진을 order로 매핑
            Map<Integer, PlacePhoto> existingPhotosByOrder = new HashMap<>();
            if (!existingPhotos.isEmpty()) {
                existingPhotosByOrder = existingPhotos.stream()
                        .collect(Collectors.toMap(
                                PlacePhoto::getOrder,
                                photo -> photo,
                                (existing, replacement) -> existing // 중복 시 기존 것 유지
                        ));
            }

            // 요청된 사진 정보로 업데이트 또는 생성
            List<PlacePhoto> photosToSave = new ArrayList<>();
            for (CreatePlacePhotoRequestDto.PhotoInfo photoInfo : request.getPhotos()) {
                Integer order = photoInfo.getOrder();
                String photoUrl = photoInfo.getPhotoUrl();
                
                // photoUrl이 빈 문자열이면 disable 처리
                if (photoUrl != null && photoUrl.trim().isEmpty()) {
                    PlacePhoto existingPhoto = existingPhotosByOrder.get(order);
                    if (existingPhoto != null) {
                        existingPhoto.disable();
                        photosToSave.add(existingPhoto);
                    }
                } else if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                    // photoUrl이 있으면 업데이트 또는 생성
                    PlacePhoto existingPhoto = existingPhotosByOrder.get(order);
                    if (existingPhoto != null) {
                        // 기존 사진이 있으면 URL만 업데이트하고 enable
                        existingPhoto.updatePhotoUrl(photoUrl);
                        existingPhoto.enable();
                        photosToSave.add(existingPhoto);
                    } else {
                        // 기존 사진이 없으면 새로 생성
                        PlacePhoto newPhoto = PlacePhoto.builder()
                                .place(savedPlace)
                                .photoUrl(photoUrl)
                                .order(order)
                                .enabled(true)
                                .build();
                        photosToSave.add(newPhoto);
                    }
                }

            }
            
            // 변경사항 저장
            if (!photosToSave.isEmpty()) {
                placePhotoRepository.saveAll(photosToSave);
            }

            log.info("장소 사진 업데이트 완료: placeId={}, processedCount={}", 
                     placeId, request.getPhotos().size());
        }

        log.info("장소 수정 완료: placeId={}, name={}", savedPlace.getId(), savedPlace.getName());

        return savedPlace.getId();
    }

    /**
     * 사용자가 관심 등록한 관광지 목록 조회
     */
    public List<SimplePlaceDto> getLikedPlaces(Long userId) {
        List<Place> likedPlaces = userPlaceRepository.findLikedPlacesByUserId(userId);

        if (likedPlaces.isEmpty()) {
            return List.of();
        }

        List<Long> placeIds = likedPlaces.stream()
                .map(Place::getId)
                .collect(Collectors.toList());

        Map<Long, String> thumbnailMap = placePhotoRepository.findFirstPhotoUrlByPlaceIdIn(placeIds).stream()
                .collect(Collectors.toMap(
                        result -> ((Number) result[0]).longValue(),
                        result -> (String) result[1]
                ));

        return likedPlaces.stream()
                .map(place -> SimplePlaceDto.builder()
                        .placeId(place.getId())
                        .name(place.getName())
                        .address(place.getAddress())
                        .thumbnailUrl(thumbnailMap.get(place.getId()))
                        .isLiked(true)
                        .groupName(place.getGroup() != null ? place.getGroup().name() : null)
                        .categoryName(place.getCategory() != null ? place.getCategory().getDisplayName() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 장소 사진 추가 (관리자용) - 여러 장 한번에 추가
     * @param placeId 장소 ID
     * @param photos 사진 정보 리스트 (photoUrl과 order 포함)
     * @return 생성된 사진 ID 리스트
     */
    @Transactional
    public List<Long> createPlacePhotos(Long placeId, List<CreatePlacePhotoRequestDto.PhotoInfo> photos) {
        // 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        // PlacePhoto 리스트 생성
        List<PlacePhoto> placePhotos = photos.stream()
                .map(photoInfo -> PlacePhoto.builder()
                        .place(place)
                        .photoUrl(photoInfo.getPhotoUrl())
                        .order(photoInfo.getOrder())
                        .enabled(true)
                        .build())
                .collect(Collectors.toList());

        // 일괄 저장
        List<PlacePhoto> savedPhotos = placePhotoRepository.saveAll(placePhotos);
        
        List<Long> photoIds = savedPhotos.stream()
                .map(PlacePhoto::getId)
                .collect(Collectors.toList());

        log.info("장소 사진 추가 완료: photoIds={}, placeId={}, count={}", 
                 photoIds, placeId, savedPhotos.size());

        return photoIds;
    }

    /**
     * 카테고리 표시 이름을 Category enum으로 변환
     * @param displayName 카테고리 표시 이름 ('자연/힐링', '역사/전통', '문화/체험', '식도락')
     * @return Category enum
     */
    private Category convertDisplayNameToCategory(String displayName) {
        if (displayName == null) {
            return null;
        }

        for (Category category : Category.values()) {
            if (category.getDisplayName().equals(displayName)) {
                return category;
            }
        }

        return null;
    }

    private static final int HOT_RECENT_DAYS = 7;
    private static final double HOT_ALPHA = 0.7;

    /**
     * HOT 관광지 조회
     */
    public List<HotPlaceDto> getHotPlaces(int limit) {
        int fetchSize = Math.min(Math.max(limit, 1), 20);

        List<Object[]> ranking = userPlaceRepository.findHotPlaces(fetchSize, HOT_RECENT_DAYS, HOT_ALPHA);
        log.info("HOT 관광지 쿼리 결과: {} 개", ranking.size());
        if (ranking.isEmpty()) {
            log.warn("HOT 관광지 없음 - enabled된 장소가 없거나 좋아요가 없습니다");
            return List.of();
        }

        List<Long> placeIds = ranking.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());
        log.info("HOT 관광지 ID 목록: {}", placeIds);

        Map<Long, Place> placeMap = placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, place -> place));
        log.info("조회된 장소 개수: {} / 요청한 ID 개수: {}", placeMap.size(), placeIds.size());

        Map<Long, String> thumbnailMap = placePhotoRepository.findFirstPhotoUrlByPlaceIdIn(placeIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> (String) row[1]
                ));

        List<HotPlaceDto> hotPlaces = new ArrayList<>();
        int rank = 1;
        for (Object[] row : ranking) {
            Long placeId = ((Number) row[0]).longValue();
            Place place = placeMap.get(placeId);
            if (place == null) {
                log.warn("장소 ID {} 를 찾을 수 없습니다", placeId);
                continue;
            }

            Long totalLikes = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            Long recentLikes = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            log.info("HOT 관광지 순위 {}: ID={}, 이름={}, 총좋아요={}, 최근좋아요={}", 
                    rank, placeId, place.getName(), totalLikes, recentLikes);

            hotPlaces.add(HotPlaceDto.builder()
                    .rank(rank++)
                    .placeId(place.getId())
                    .name(place.getName())
                    .address(place.getAddress())
                    .thumbnailUrl(thumbnailMap.get(placeId))
                    .categoryName(place.getCategory() != null ? place.getCategory().getDisplayName() : null)
                    .totalLikes(totalLikes)
                    .recentLikes(recentLikes)
                    .build());

            if (hotPlaces.size() >= limit) {
                break;
            }
        }

        log.info("최종 반환할 HOT 관광지 개수: {}", hotPlaces.size());
        return hotPlaces;
    }
}

