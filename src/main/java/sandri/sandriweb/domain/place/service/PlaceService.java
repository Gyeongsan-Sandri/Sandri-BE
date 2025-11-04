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
     * Place ID로 근처 가볼만한 곳 조회 (카테고리별)
     * @param placeId 기준 관광지 ID
     * @param categoryName 카테고리 이름 (관광지/맛집/카페)
     * @param limit 조회할 개수
     * @return 근처 관광지 리스트
     */
    public List<NearbyPlaceDto> getNearbyPlacesByPlaceId(Long placeId, String categoryName, int limit) {
        // 1. 카테고리 검증
        try {
            PlaceCategory.valueOf(categoryName);
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
                categoryName,
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
                            .name(nearbyPlace.getName())
                            .thumbnailUrl(thumbnailUrl)
                            .distanceInMeters(distance)
                            .categoryName(nearbyPlace.getCategory() != null ? nearbyPlace.getCategory().getDisplayName() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Place ID로 근처 가볼만한 곳 조회 (카테고리 필터 없음)
     * @param placeId 기준 관광지 ID
     * @param limit 조회할 개수
     * @return 근처 관광지 리스트
     */
    public List<NearbyPlaceDto> getNearbyPlaces(Long placeId, int limit) {
        // 1. 기준 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));
        
        // 2. 5km 반경 (미터 단위)
        double radius = 5000.0;
        
        // 3. 근처 장소 조회 (카테고리 필터 없음)
        List<Place> nearbyPlaces = placeRepository.findNearbyPlaces(
                place.getLocation(),
                radius,
                place.getId(),
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
                            .name(nearbyPlace.getName())
                            .thumbnailUrl(thumbnailUrl)
                            .distanceInMeters(distance)
                            .categoryName(nearbyPlace.getCategory() != null ? nearbyPlace.getCategory().getDisplayName() : null)
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
     * 장소 사진 목록 조회 (커서 기반 페이징)
     * @param lastPlaceId 마지막으로 조회한 place_id (첫 조회시 null)
     * @param size 페이지 크기
     * @return 커서 기반 페이징된 장소 사진 목록
     */
    @Transactional(readOnly = true)
    public PlacePhotoCursorResponseDto getPlacePhotosByCursor(Long lastPlaceId, int size) {
        // size + 1개 조회하여 다음 페이지 여부 판단
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Object[]> allPhotos = placePhotoRepository.findFirstPhotoUrlByPlaceIdWithCursor(lastPlaceId, pageable);
        
        boolean hasNext = allPhotos.size() > size;
        List<Object[]> pageItems = hasNext ? allPhotos.subList(0, size) : allPhotos;
        
        // DTO 변환
        List<PlacePhotoCursorResponseDto.PlacePhotoDto> photoDtos = pageItems.stream()
                .map(result -> PlacePhotoCursorResponseDto.PlacePhotoDto.builder()
                        .placeId(((Number) result[0]).longValue())
                        .photoUrl((String) result[1])
                        .build())
                .collect(Collectors.toList());
        
        // 다음 커서 설정
        Long nextCursor = null;
        if (hasNext && !pageItems.isEmpty()) {
            nextCursor = ((Number) pageItems.get(pageItems.size() - 1)[0]).longValue();
        }
        
        return PlacePhotoCursorResponseDto.builder()
                .photos(photoDtos)
                .size(size)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
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
    public List<CategoryPlaceDto> getPlacesByCategory(String categoryDisplayName, int count, Long userId) {
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

        // 좋아요 수 계산
        Map<Long, Long> likeCountByPlaceId = userPlaceRepository.countLikesByPlaceIds(placeIds).stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));

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

        // 평점 계산 (배치 조회로 N+1 문제 해결)
        Map<Long, Double> ratingByPlaceId = reviewService.getAverageRatingsByPlaceIds(placeIds);

        // DTO 변환
        return places.stream()
                .map(place -> {
                    // 사진 URL 추출 (각 장소당 한 장씩)
                    String thumbnailUrl = photoUrlByPlaceId.get(place.getId());

                    // 좋아요 수
                    Integer likeCount = likeCountByPlaceId.getOrDefault(place.getId(), 0L).intValue();

                    // 사용자가 좋아요한 장소인지 확인
                    Boolean isLiked = userId != null ? likedPlaceIds.getOrDefault(place.getId(), false) : null;

                    // 평점
                    Double rating = ratingByPlaceId.getOrDefault(place.getId(), 0.0);

                    return CategoryPlaceDto.builder()
                            .placeId(place.getId())
                            .name(place.getName())
                            .address(place.getAddress())
                            .thumbnailUrl(thumbnailUrl)
                            .rating(rating)
                            .likeCount(likeCount)
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
        Integer maxOrder = placePhotoRepository.findMaxOrderByPlaceId(request.getPlaceId());
        int nextOrder = (maxOrder == null || maxOrder == -1) ? 0 : maxOrder + 1;

        // PlacePhoto 생성
        PlacePhoto photo = PlacePhoto.builder()
                .place(place)
                .photoUrl(request.getPhotoUrl())
                .order(nextOrder)
                .build();

        PlacePhoto savedPhoto = placePhotoRepository.save(photo);
        log.info("장소 사진 추가 완료: photoId={}, placeId={}, order={}", 
                 savedPhoto.getId(), request.getPlaceId(), savedPhoto.getOrder());

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

