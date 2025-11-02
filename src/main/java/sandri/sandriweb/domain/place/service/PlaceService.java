package sandri.sandriweb.domain.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.place.dto.*;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.entity.PlacePhoto;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.repository.PlacePhotoRepository;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.place.repository.UserPlaceRepository;
import sandri.sandriweb.domain.review.service.ReviewService;

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
    
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 관광지 상세 정보 조회
     * @param placeId 관광지 ID
     * @param reviewCount 조회할 리뷰 개수
     * @param reviewPhotoCount 조회할 리뷰 사진 개수
     * @param reviewSort 리뷰 정렬 기준 (latest: 최신순, rating_high: 평점 높은 순, rating_low: 평점 낮은 순)
     * @return PlaceDetailResponseDto
     */
    public PlaceDetailResponseDto getPlaceDetail(Long placeId, int reviewCount, int reviewPhotoCount, String reviewSort) {
        // 1. 관광지 기본 정보 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));

        // 2. 공식 사진 조회
        List<String> officialPhotos = placePhotoRepository.findByPlaceId(placeId).stream()
                .map(PlacePhoto::getPhotoUrl)
                .collect(Collectors.toList());

        // 3. 평점 계산
        Double averageRating = reviewService.getAverageRating(placeId);

        // 4. 리뷰 조회 (요청 개수만큼)
        List<ReviewDto> reviewDtos = reviewService.getReviewsByPlaceId(placeId, reviewCount, reviewSort);

        // 5. 리뷰 사진 조회 (요청 개수만큼)
        List<String> reviewPhotoUrls = reviewService.getReviewPhotos(placeId, reviewPhotoCount);

        // 6. 근처 가볼만한 곳 조회 (기본: 관광지 3곳)
        List<NearbyPlaceDto> nearbyPlaces = getNearbyPlaces(place, "관광지", 3);

        // 7. DTO 생성 및 반환
        return PlaceDetailResponseDto.builder()
                .placeId(place.getId())
                .name(place.getName())
                .address(place.getAddress())
                .groupName(place.getGroup() != null ? place.getGroup().name() : null)
                .categoryName(place.getCategory() != null ? place.getCategory().getDisplayName() : null)
                .rating(averageRating)
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .phone(place.getPhone())
                .webpage(place.getWebpage())
                .summary(place.getSummery())
                .information(place.getInformation())
                .officialPhotos(officialPhotos)
                .reviewPhotos(reviewPhotoUrls)
                .reviews(reviewDtos)
                .nearbyPlaces(nearbyPlaces)
                .build();
    }

    /**
     * 근처 가볼만한 곳 조회 (카테고리별)
     * @param place 기준 관광지
     * @param categoryName 카테고리 이름 (관광지/맛집/카페)
     * @param limit 조회할 개수
     * @return 근처 관광지 리스트
     */
    public List<NearbyPlaceDto> getNearbyPlaces(Place place, String categoryName, int limit) {
        // 5km 반경 (미터 단위)
        double radius = 5000.0;
        
        // 카테고리별 근처 장소 조회
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
        
        // 근처 장소들의 ID 추출
        List<Long> nearbyPlaceIds = nearbyPlaces.stream()
                .map(Place::getId)
                .collect(Collectors.toList());
        
        // 근처 장소들을 다시 조회 (효율적인 로딩)
        List<Place> placesWithCategory = nearbyPlaceIds.stream()
                .map(id -> placeRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
        
        // 사진 효율적으로 로딩
        List<PlacePhoto> allNearbyPhotos = placePhotoRepository.findByPlaceIdIn(nearbyPlaceIds);
        
        // Place ID별로 사진 그룹화
        java.util.Map<Long, List<PlacePhoto>> photosByPlaceId = allNearbyPhotos.stream()
                .collect(Collectors.groupingBy(photo -> photo.getPlace().getId()));
        
        // 기준 장소의 위치
        Point centerLocation = place.getLocation();
        
        return placesWithCategory.stream()
                .map(nearbyPlace -> {
                    // 사진 URL 추출
                    List<PlacePhoto> photos = photosByPlaceId.getOrDefault(nearbyPlace.getId(), List.of());
                    String thumbnailUrl = photos.isEmpty() ? null : photos.get(0).getPhotoUrl();
                    
                    // 거리 계산 (미터 단위)
                    Long distance = calculateDistanceInMeters(centerLocation, nearbyPlace.getLocation());
                    
                    return NearbyPlaceDto.builder()
                            .name(nearbyPlace.getName())
                            .thumbnailUrl(thumbnailUrl)
                            .distanceInMeters(distance)
                            .categoryName(nearbyPlace.getCategory() != null ? nearbyPlace.getCategory().getDisplayName() : null) // 세부 카테고리
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
     * Place ID로 근처 가볼만한 곳 조회
     * @param placeId 기준 관광지 ID
     * @param categoryName 카테고리 이름
     * @param limit 조회할 개수
     * @return 근처 관광지 리스트
     */
    public List<NearbyPlaceDto> getNearbyPlacesByPlaceId(Long placeId, String categoryName, int limit) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));
        return getNearbyPlaces(place, categoryName, limit);
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

        // 사진 효율적으로 로딩
        List<PlacePhoto> allPhotos = placePhotoRepository.findByPlaceIdIn(placeIds);
        Map<Long, List<PlacePhoto>> photosByPlaceId = allPhotos.stream()
                .collect(Collectors.groupingBy(photo -> photo.getPlace().getId()));

        // 좋아요 수 계산 (효율적으로)
        Map<Long, Long> likeCountByPlaceId = userPlaceRepository.countLikesByPlaceIds(placeIds).stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));

        // 사용자가 좋아요한 장소 ID 조회 (로그인한 경우)
        Map<Long, Boolean> likedPlaceIds = new java.util.HashMap<>();
        if (userId != null) {
            List<Long> likedIds = userPlaceRepository.findLikedPlaceIdsByUserId(userId, placeIds);
            likedPlaceIds = likedIds.stream()
                    .collect(Collectors.toMap(
                            placeId -> placeId,
                            placeId -> true
                    ));
        }

        // 평점 계산
        Map<Long, Double> ratingByPlaceId = places.stream()
                .collect(Collectors.toMap(
                        Place::getId,
                        place -> reviewService.getAverageRating(place.getId())
                ));

        // DTO 변환
        return places.stream()
                .map(place -> {
                    // 사진 URL 추출
                    List<PlacePhoto> photos = photosByPlaceId.getOrDefault(place.getId(), List.of());
                    String thumbnailUrl = photos.isEmpty() ? null : photos.get(0).getPhotoUrl();

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

