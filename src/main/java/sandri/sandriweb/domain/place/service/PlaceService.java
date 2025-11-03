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

    /**
     * 관광지 상세 정보 조회 (기본 정보만, 리뷰 제외)
     * @param placeId 관광지 ID
     * @return PlaceDetailResponseDto (리뷰 정보 제외)
     */
    public PlaceDetailResponseDto getPlaceDetail(Long placeId) {
        // 1. 관광지 기본 정보 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("관광지를 찾을 수 없습니다."));

        // 2. 공식 사진 조회
        List<String> officialPhotos = placePhotoRepository.findByPlaceId(placeId).stream()
                .map(PlacePhoto::getPhotoUrl)
                .collect(Collectors.toList());

        // 3. 평점 계산
        Double averageRating = reviewService.getAverageRating(placeId);

        // 4. DTO 생성 및 반환 (리뷰, 근처 장소는 별도 API로 조회)
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
        
        // 사진 효율적으로 로딩 (각 장소당 첫 번째 사진만)
        List<PlacePhoto> firstPhotos = placePhotoRepository.findFirstPhotoByPlaceIdIn(nearbyPlaceIds);
        
        // Place ID별로 사진 매핑 (각 장소당 한 장씩)
        java.util.Map<Long, String> photoUrlByPlaceId = firstPhotos.stream()
                .collect(Collectors.toMap(
                        photo -> photo.getPlace().getId(),
                        PlacePhoto::getPhotoUrl
                ));
        
        // 기준 장소의 위치
        Point centerLocation = place.getLocation();
        
        return placesWithCategory.stream()
                .map(nearbyPlace -> {
                    // 사진 URL 추출 (각 장소당 한 장씩)
                    String thumbnailUrl = photoUrlByPlaceId.get(nearbyPlace.getId());
                    
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

        // 사진 효율적으로 로딩 (각 장소당 첫 번째 사진만)
        List<PlacePhoto> firstPhotos = placePhotoRepository.findFirstPhotoByPlaceIdIn(placeIds);
        Map<Long, String> photoUrlByPlaceId = firstPhotos.stream()
                .collect(Collectors.toMap(
                        photo -> photo.getPlace().getId(),
                        PlacePhoto::getPhotoUrl
                ));

        // 좋아요 수 계산 (효율적으로)
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

        // 평점 계산
        Map<Long, Double> ratingByPlaceId = places.stream()
                .collect(Collectors.toMap(
                        Place::getId,
                        place -> reviewService.getAverageRating(place.getId())
                ));

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
        // 장소 존재 확인
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다."));
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
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
                    // 좋아요가 없는 경우: 새로 생성
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
                .phone(request.getPhone())
                .webpage(request.getWebpage())
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

        // PlacePhoto 생성
        PlacePhoto photo = PlacePhoto.builder()
                .place(place)
                .photoUrl(photoUrl)
                .build();

        PlacePhoto savedPhoto = placePhotoRepository.save(photo);
        log.info("장소 사진 추가 완료: photoId={}, placeId={}", savedPhoto.getId(), placeId);

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

