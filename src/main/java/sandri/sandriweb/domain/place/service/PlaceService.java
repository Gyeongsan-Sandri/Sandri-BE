package sandri.sandriweb.domain.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;
import java.util.Map;
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

        // 2. 공식 사진 조회 (order 순서대로)
        List<PlaceDetailResponseDto.PhotoDto> officialPhotos = placePhotoRepository.findByPlaceId(placeId).stream()
                .map(photo -> PlaceDetailResponseDto.PhotoDto.builder()
                        .order(photo.getOrder())
                        .photoUrl(photo.getPhotoUrl())
                        .build())
                .collect(Collectors.toList());

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
     * Place ID로 근처 가볼만한 곳 조회 (카테고리별)
     * @param placeId 기준 관광지 ID
     * @param groupName 카테고리 이름 (관광지/맛집/카페)
     * @param limit 조회할 개수
     * @return 근처 관광지 리스트
     */
    public List<NearbyPlaceDto> getNearbyPlacesByPlaceId(Long placeId, String groupName, int limit) {
        // 1. 카테고리 검증
        try {
            PlaceCategory.valueOf(groupName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("카테고리는 '관광지', '맛집', '카페' 중 하나여야 합니다.");
        }
        
        // 2. 기준 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));
        
        // 3. 5km 반경 (미터 단위)
        double radius = 5000.0;
        
        // 4. 카테고리별 근처 장소 조회
        List<Place> nearbyPlaces = placeRepository.findNearbyPlacesByCategory(
                place.getLocation(),
                radius,
                place.getId(),
                groupName,
                limit
        );
        
        if (nearbyPlaces.isEmpty()) {
            return List.of();
        }
        
        // 4. 사진 조회 및 매핑 (공통 헬퍼 메서드 사용)
        List<Long> nearbyPlaceIds = nearbyPlaces.stream()
                .map(Place::getId)
                .collect(Collectors.toList());
        Map<Long, String> photoUrlByPlaceId = getPhotoUrlByPlaceIds(nearbyPlaceIds);
        
        // 5. 기준 장소의 위치
        Point centerLocation = place.getLocation();
        
        // 6. DTO 변환
        return nearbyPlaces.stream()
                .map(nearbyPlace -> {
                    String thumbnailUrl = photoUrlByPlaceId.get(nearbyPlace.getId());
                    Long distance = calculateDistanceInMeters(centerLocation, nearbyPlace.getLocation());
                    
                    return NearbyPlaceDto.builder()
                            .placeId(nearbyPlace.getId())
                            .name(nearbyPlace.getName())
                            .thumbnailUrl(thumbnailUrl)
                            .distanceInMeters(distance)
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
     * 카테고리별 장소 조회 (좋아요 많은 순)
     * @param categoryDisplayName 카테고리 표시 이름 ('자연/힐링', '역사/전통', '문화/체험', '식도락')
     * @param count 조회할 개수
     * @param userId 사용자 ID (로그인한 경우에만 제공, null 가능)
     * @return 카테고리별 장소 리스트
     */
    @Transactional(readOnly = true)
    public List<SimplePlaceDto> getPlacesByCategory(String categoryDisplayName, int count, Long userId) {
        // 카테고리 표시 이름을 enum으로 변환
        Category category = convertDisplayNameToCategory(categoryDisplayName);
        if (category == null) {
            throw new RuntimeException("유효하지 않은 카테고리입니다: " + categoryDisplayName);
        }

        // 카테고리별 장소 조회 (좋아요 많은 순)
        List<Place> places = placeRepository.findByCategoryOrderByLikeCountDesc(category.name(), count);

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
     * 장소 생성 (관리자용)
     * @param request 장소 생성 요청 DTO
     * @return 생성된 장소 ID
     */
    @Transactional
    public Long createPlace(CreatePlaceRequestDto request) {
        // 중복 검사
        if (placeRepository.existsByName(request.getName())) {
            throw new RuntimeException("이미 존재하는 장소 이름입니다: " + request.getName());
        }

        // 위도/경도를 Point로 변환
        Point location = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(request.getLongitude(), request.getLatitude())
        );

        // Place 생성
        Place place = Place.builder()
                .name(request.getName())
                .address(request.getAddress())
                .location(location)
                .summery(request.getSummary())
                .information(request.getInformation())
                .group(request.getGroup())
                .category(request.getCategory())
                .build();

        Place savedPlace = placeRepository.save(place);
        log.info("장소 생성 완료: placeId={}, name={}", savedPlace.getId(), savedPlace.getName());

        return savedPlace.getId();
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
        log.info("장소 수정 완료: placeId={}, name={}", savedPlace.getId(), savedPlace.getName());

        return savedPlace.getId();
    }

    /**
     * 장소 사진 추가 (관리자용)
     * @param placeId 장소 ID
     * @param photoUrl 사진 URL
     * @return 생성된 사진 ID
     */
    @Transactional
    public Long createPlacePhoto(Long placeId, String photoUrl) {
        // 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));

        // 최대 order 값 조회 (새 사진의 order = MAX(order) + 1)
        Integer maxOrder = placePhotoRepository.findMaxOrderByPlaceId(placeId);
        int nextOrder = (maxOrder == null || maxOrder == -1) ? 0 : maxOrder + 1;

        // PlacePhoto 생성
        PlacePhoto photo = PlacePhoto.builder()
                .place(place)
                .photoUrl(photoUrl)
                .order(nextOrder)
                .build();

        PlacePhoto savedPhoto = placePhotoRepository.save(photo);
        log.info("장소 사진 추가 완료: photoId={}, placeId={}, order={}", 
                 savedPhoto.getId(), placeId, savedPhoto.getOrder());

        return savedPhoto.getId();
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

}

