package sandri.sandriweb.domain.route.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.favorite.dto.FavoriteRouteDto;
import sandri.sandriweb.domain.route.dto.*;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.entity.RouteLocation;
import sandri.sandriweb.domain.route.entity.RouteParticipant;
import sandri.sandriweb.domain.route.entity.UserRoute;
import sandri.sandriweb.domain.route.enums.RouteSortType;
import sandri.sandriweb.domain.route.repository.RouteLocationRepository;
import sandri.sandriweb.domain.route.repository.RouteParticipantRepository;
import sandri.sandriweb.domain.route.repository.RouteRepository;
import sandri.sandriweb.domain.route.repository.UserRouteRepository;
import sandri.sandriweb.domain.route.util.QrCodeGenerator;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.place.repository.PlacePhotoRepository;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.entity.PlacePhoto;
import sandri.sandriweb.domain.place.enums.PlaceCategory;
import sandri.sandriweb.domain.place.enums.Category;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RouteService {
    
    private final RouteRepository routeRepository;
    private final RouteParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final UserRouteRepository userRouteRepository;
    private final RouteLocationRepository routeLocationRepository;
    private final PlaceRepository placeRepository;
    private final PlacePhotoRepository placePhotoRepository;
    
    @Value("${app.base-url}")
    private String baseUrl;

    private static final int HOT_RECENT_DAYS = 7;
    private static final double HOT_ALPHA = 0.7;
    
    @Transactional
    public ApiResponseDto<RouteResponseDto> createRoute(CreateRouteRequestDto request, User creator) {
        try {
            log.info("루트 생성 요청 상세: 제목={}, 공개여부 요청값={}", request.getTitle(), request.isPublic());
            
            // 이미지 URL 처리: 사용자가 제공하지 않으면 첫 번째 장소 사진 사용
            String imageUrl = normalizeImageUrl(request.getImageUrl());
            if (imageUrl == null && request.getLocations() != null && !request.getLocations().isEmpty()) {
                imageUrl = findFirstPlacePhoto(request.getLocations().get(0).getName());
            }
            
            Route route = Route.builder()
                    .title(request.getTitle())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .creator(creator)
                    .isPublic(request.isPublic())
                    .imageUrl(imageUrl)
                    .build();
            
            log.info("루트 엔티티 생성: 제목={}, 공개여부={}", route.getTitle(), route.isPublic());
            
            // 위치 정보 추가
            if (request.getLocations() != null && !request.getLocations().isEmpty()) {
                List<RouteLocation> locations = request.getLocations().stream()
                        .map(locDto -> {
                            RouteLocation location = RouteLocation.builder()
                                    .route(route)
                                    .dayNumber(locDto.getDayNumber())
                                    .name(locDto.getName())
                                    .address(locDto.getAddress())
                                    .latitude(locDto.getLatitude() != null ? BigDecimal.valueOf(locDto.getLatitude()) : null)
                                    .longitude(locDto.getLongitude() != null ? BigDecimal.valueOf(locDto.getLongitude()) : null)
                                    .description(locDto.getDescription())
                                    .displayOrder(locDto.getDisplayOrder() != null ? locDto.getDisplayOrder() : 0)
                                    .memo(normalizeMemo(locDto.getMemo()))
                                    .build();
                            return location;
                        })
                        .collect(Collectors.toList());
                
                route.getLocations().addAll(locations);
            }
            
            Route savedRoute = routeRepository.save(route);
            
            // 생성자를 일행에 추가
            RouteParticipant creatorParticipant = RouteParticipant.create(savedRoute, creator);
            participantRepository.save(creatorParticipant);
            
            RouteResponseDto response = RouteResponseDto.from(savedRoute);
            return ApiResponseDto.success("루트가 생성되었습니다", response);
            
        } catch (Exception e) {
            log.error("루트 생성 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error("루트 생성에 실패했습니다: " + e.getMessage());
        }
    }
    
    public ApiResponseDto<RouteResponseDto> getRoute(Long routeId, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            // 비공개 루트인 경우 권한 확인
            if (!route.isPublic() && 
                !route.getCreator().getId().equals(user.getId()) && 
                !participantRepository.existsByRouteAndUser(route, user)) {
                throw new RuntimeException("접근 권한이 없습니다");
            }
            
            RouteResponseDto response = RouteResponseDto.from(route);
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("루트 조회 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<RouteResponseDto> updateRoute(Long routeId, UpdateRouteRequestDto request, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            if (!route.getCreator().getId().equals(user.getId())) {
                throw new RuntimeException("수정 권한이 없습니다");
            }
            
            if (request.getTitle() != null) {
                route.updateTitle(request.getTitle());
            }
            if (request.getStartDate() != null && request.getEndDate() != null) {
                route.updateDates(request.getStartDate(), request.getEndDate());
            }
            if (request.getIsPublic() != null) {
                route.updateVisibility(request.getIsPublic());
            }
            if (request.getImageUrl() != null) {
                String imageUrl = normalizeImageUrl(request.getImageUrl());
                // 이미지 URL이 비어있고 위치가 있으면 첫 번째 장소 사진 사용
                if (imageUrl == null && request.getLocations() != null && !request.getLocations().isEmpty()) {
                    imageUrl = findFirstPlacePhoto(request.getLocations().get(0).getName());
                }
                route.updateImageUrl(imageUrl);
            }
            
            // 위치 정보 업데이트
            if (request.getLocations() != null) {
                route.getLocations().clear();
                List<RouteLocation> locations = request.getLocations().stream()
                        .map(locDto -> {
                            RouteLocation location = RouteLocation.builder()
                                    .route(route)
                                    .dayNumber(locDto.getDayNumber())
                                    .name(locDto.getName())
                                    .address(locDto.getAddress())
                                    .latitude(locDto.getLatitude() != null ? BigDecimal.valueOf(locDto.getLatitude()) : null)
                                    .longitude(locDto.getLongitude() != null ? BigDecimal.valueOf(locDto.getLongitude()) : null)
                                    .description(locDto.getDescription())
                                    .displayOrder(locDto.getDisplayOrder() != null ? locDto.getDisplayOrder() : 0)
                                    .memo(normalizeMemo(locDto.getMemo()))
                                    .build();
                            return location;
                        })
                        .collect(Collectors.toList());
                
                route.getLocations().addAll(locations);
            }
            
            RouteResponseDto response = RouteResponseDto.from(route);
            return ApiResponseDto.success("루트가 수정되었습니다", response);
            
        } catch (Exception e) {
            log.error("루트 수정 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<Void> deleteRoute(Long routeId, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            if (!route.getCreator().getId().equals(user.getId())) {
                throw new RuntimeException("삭제 권한이 없습니다");
            }
            
            routeRepository.delete(route);
            return ApiResponseDto.success("루트가 삭제되었습니다", null);
            
        } catch (Exception e) {
            log.error("루트 삭제 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    public ApiResponseDto<List<RouteListDto>> getUserRoutes(User user, RouteSortType sortType) {
        try {
            List<Route> routes = routeRepository.findByParticipantOrCreator(user);

            List<UserRoute> likedRoutes = userRouteRepository.findAllEnabledByUserId(user.getId());
            Map<Long, UserRoute> likedRouteMap = likedRoutes.stream()
                    .collect(Collectors.toMap(
                            ur -> ur.getRoute().getId(),
                            ur -> ur,
                            (existing, duplicate) -> existing));

            Comparator<Route> comparator = buildComparator(sortType, likedRouteMap);
            routes.sort(comparator);

            List<RouteListDto> response = routes.stream()
                    .map(route -> RouteListDto.from(route, likedRouteMap.containsKey(route.getId())))
                    .collect(Collectors.toList());
            
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("사용자 루트 조회 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }

    @Transactional
    public boolean toggleLike(Long routeId, Long userId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다."));
        
        log.info("루트 좋아요 토글: 루트ID={}, 제목={}, 공개여부={}, 사용자ID={}", 
                routeId, route.getTitle(), route.isPublic(), userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        return userRouteRepository.findByUserIdAndRouteId(userId, routeId)
                .map(userRoute -> {
                    if (userRoute.isEnabled()) {
                        userRoute.disable();
                        userRouteRepository.save(userRoute);
                        log.info("루트 좋아요 취소: 루트ID={}", routeId);
                        return false;
                    } else {
                        userRoute.enable();
                        userRouteRepository.save(userRoute);
                        log.info("루트 좋아요 재활성화: 루트ID={}", routeId);
                        return true;
                    }
                })
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    UserRoute newUserRoute = UserRoute.builder()
                            .user(user)
                            .route(route)
                            .build();
                    userRouteRepository.save(newUserRoute);
                    log.info("루트 좋아요 신규 등록: 루트ID={}, enabled={}", routeId, newUserRoute.isEnabled());
                    return true;
                });
    }

    public List<FavoriteRouteDto> getLikedRoutes(Long userId) {
        List<Route> routes = userRouteRepository.findLikedRoutesByUserId(userId);
        if (routes.isEmpty()) {
            return List.of();
        }

        return routes.stream()
                .map(FavoriteRouteDto::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ApiResponseDto<RouteResponseDto> addParticipant(Long routeId, AddParticipantRequestDto request, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            // 권한 확인: 생성자 또는 이미 참여 중인 사용자만 일행 추가 가능
            boolean isCreator = route.getCreator().getId().equals(user.getId());
            boolean isParticipant = participantRepository.existsByRouteAndUser(route, user);
            
            if (!isCreator && !isParticipant) {
                throw new RuntimeException("일행 추가 권한이 없습니다");
            }
            
            // userId 유효성 검증
            if (request.getUserId() == null || request.getUserId() <= 0) {
                throw new RuntimeException("유효하지 않은 사용자 ID입니다. 사용자 ID는 1 이상이어야 합니다.");
            }
            
            User participantUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("사용자 ID " + request.getUserId() + "에 해당하는 사용자를 찾을 수 없습니다"));
            
            if (participantRepository.existsByRouteAndUser(route, participantUser)) {
                throw new RuntimeException("이미 일행으로 등록된 사용자입니다");
            }
            
            RouteParticipant participant = RouteParticipant.create(route, participantUser);
            participantRepository.save(participant);
            
            Route updatedRoute = routeRepository.findById(routeId).orElse(route);
            RouteResponseDto response = RouteResponseDto.from(updatedRoute);
            return ApiResponseDto.success("일행이 추가되었습니다", response);
            
        } catch (Exception e) {
            log.error("일행 추가 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    public ApiResponseDto<List<RouteResponseDto.ParticipantDto>> getParticipants(Long routeId, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            // 권한 확인
            boolean isCreator = route.getCreator().getId().equals(user.getId());
            boolean isParticipant = participantRepository.existsByRouteAndUser(route, user);
            
            if (!isCreator && !isParticipant) {
                throw new RuntimeException("접근 권한이 없습니다");
            }
            
            List<RouteParticipant> participants = participantRepository.findByRoute(route);
            List<RouteResponseDto.ParticipantDto> response = participants.stream()
                    .map(RouteResponseDto.ParticipantDto::from)
                    .collect(Collectors.toList());
            
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("일행 조회 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<Void> deleteParticipants(Long routeId, List<Long> participantIds, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            if (!route.getCreator().getId().equals(user.getId())) {
                throw new RuntimeException("일행 삭제 권한이 없습니다");
            }
            
            for (Long participantId : participantIds) {
                RouteParticipant participant = participantRepository.findById(participantId)
                        .orElseThrow(() -> new RuntimeException("일행을 찾을 수 없습니다: " + participantId));
                
                if (!participant.getRoute().getId().equals(routeId)) {
                    throw new RuntimeException("잘못된 일행 정보입니다");
                }
                
                // 생성자는 삭제할 수 없음
                if (participant.getUser().getId().equals(route.getCreator().getId())) {
                    continue;
                }
                
                participantRepository.delete(participant);
            }
            
            return ApiResponseDto.success("일행이 삭제되었습니다", null);
            
        } catch (Exception e) {
            log.error("일행 삭제 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<RouteResponseDto.LocationDto> addLocation(Long routeId, AddLocationRequestDto request, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));

            if (!hasRouteAccess(route, user)) {
                throw new RuntimeException("장소 추가 권한이 없습니다");
            }

            // dayNumber 유효성 검증 (1 이상이어야 함)
            if (request.getDayNumber() == null || request.getDayNumber() < 1) {
                throw new RuntimeException("일차 번호는 1 이상이어야 합니다");
            }

            // Place 조회 또는 생성
            Place place;
            if (request.getPlaceId() != null) {
                // DB에 있는 장소 사용
                place = placeRepository.findById(request.getPlaceId())
                        .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다"));
            } else if (request.getGooglePlaceInfo() != null) {
                // Google Places 정보로 Place 생성
                place = createPlaceFromGooglePlaces(request.getGooglePlaceInfo());
            } else {
                throw new RuntimeException("placeId 또는 googlePlaceInfo 중 하나는 필수입니다");
            }

            // displayOrder가 지정되지 않은 경우, 해당 dayNumber의 마지막 순서로 설정
            Integer displayOrder = request.getDisplayOrder();
            if (displayOrder == null) {
                List<RouteLocation> existingLocations = routeLocationRepository
                        .findByRouteAndDayNumberOrderByDisplayOrderAsc(route, request.getDayNumber());
                displayOrder = existingLocations.isEmpty() ? 0 : 
                        existingLocations.get(existingLocations.size() - 1).getDisplayOrder() + 1;
            }

            // Place 정보를 사용하여 RouteLocation 생성
            RouteLocation location = RouteLocation.builder()
                    .route(route)
                    .dayNumber(request.getDayNumber())
                    .name(place.getName())
                    .address(place.getAddress())
                    .latitude(place.getLatitude() != null ? BigDecimal.valueOf(place.getLatitude()) : null)
                    .longitude(place.getLongitude() != null ? BigDecimal.valueOf(place.getLongitude()) : null)
                    .description(place.getSummery())
                    .displayOrder(displayOrder)
                    .memo(normalizeMemo(request.getMemo()))
                    .build();

            RouteLocation savedLocation = routeLocationRepository.save(location);

            RouteResponseDto.LocationDto response = RouteResponseDto.LocationDto.from(savedLocation);
            return ApiResponseDto.success("장소가 추가되었습니다", response);

        } catch (Exception e) {
            log.error("장소 추가 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }

    /**
     * Google Places 정보로 Place 생성
     * 호출하는 메서드의 트랜잭션을 사용합니다 (@Transactional이 private 메서드에서는 동작하지 않음)
     */
    private Place createPlaceFromGooglePlaces(AddLocationRequestDto.GooglePlaceInfo googlePlaceInfo) {
        // 이름과 좌표 필수 검증
        if (googlePlaceInfo.getName() == null || googlePlaceInfo.getName().trim().isEmpty()) {
            throw new RuntimeException("장소 이름은 필수입니다");
        }
        if (googlePlaceInfo.getLatitude() == null || googlePlaceInfo.getLongitude() == null) {
            throw new RuntimeException("위도와 경도는 필수입니다");
        }

        // 이미 같은 이름의 장소가 있으면 기존 장소 반환
        String placeName = googlePlaceInfo.getName().trim();
        java.util.Optional<Place> existingPlace = placeRepository.findByName(placeName);
        if (existingPlace.isPresent()) {
            log.info("기존 장소 사용: placeId={}, name={}", existingPlace.get().getId(), placeName);
            return existingPlace.get();
        }

        // Google Places types에서 카테고리 추출
        PlaceCategory group = extractGroupFromTypes(googlePlaceInfo.getTypes());
        Category category = extractCategoryFromTypes(googlePlaceInfo.getTypes());

        // 좌표 생성
        org.locationtech.jts.geom.Coordinate coordinate = new org.locationtech.jts.geom.Coordinate(
                googlePlaceInfo.getLongitude(),
                googlePlaceInfo.getLatitude()
        );
        org.locationtech.jts.geom.Point location = new org.locationtech.jts.geom.GeometryFactory(
                new org.locationtech.jts.geom.PrecisionModel(), 4326
        ).createPoint(coordinate);

        // Place 생성
        Place place = Place.builder()
                .name(placeName)
                .address(googlePlaceInfo.getAddress())
                .location(location)
                .summery(null)
                .information(null)
                .group(group)
                .category(category)
                .build();

        Place savedPlace = placeRepository.save(place);
        log.info("Google Places 정보로 장소 생성 완료: placeId={}, name={}", savedPlace.getId(), placeName);

        // 사진이 있으면 PlacePhoto 추가
        if (googlePlaceInfo.getPhotoUrl() != null && !googlePlaceInfo.getPhotoUrl().trim().isEmpty()) {
            PlacePhoto placePhoto = PlacePhoto.builder()
                    .place(savedPlace)
                    .photoUrl(googlePlaceInfo.getPhotoUrl().trim())
                    .order(0)
                    .build();
            placePhotoRepository.save(placePhoto);
            log.info("Google Places 사진 추가: placeId={}, photoUrl={}", savedPlace.getId(), googlePlaceInfo.getPhotoUrl());
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

    @Transactional
    public ApiResponseDto<RouteResponseDto.LocationDto> upsertLocationMemo(Long routeId, Long locationId, String memo, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));

            if (!hasRouteAccess(route, user)) {
                throw new RuntimeException("메모 수정 권한이 없습니다");
            }

            RouteLocation location = routeLocationRepository.findById(locationId)
                    .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다"));

            if (!location.getRoute().getId().equals(routeId)) {
                throw new RuntimeException("요청한 루트에 속한 장소가 아닙니다");
            }

            String normalizedMemo = normalizeMemo(memo);
            location.updateMemo(normalizedMemo);

            RouteResponseDto.LocationDto response = RouteResponseDto.LocationDto.from(location);
            String message = normalizedMemo == null ? "장소 메모가 삭제되었습니다" : "장소 메모가 저장되었습니다";

            return ApiResponseDto.success(message, response);

        } catch (Exception e) {
            log.error("장소 메모 저장 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }

    @Transactional
    public ApiResponseDto<RouteResponseDto.LocationDto> deleteLocationMemo(Long routeId, Long locationId, User user) {
        return upsertLocationMemo(routeId, locationId, null, user);
    }
    
    public ApiResponseDto<ShareLinkResponseDto> getShareLink(Long routeId, User user) {
        try {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            // 권한 확인
            boolean isCreator = route.getCreator().getId().equals(user.getId());
            boolean isParticipant = participantRepository.existsByRouteAndUser(route, user);
            
            if (!isCreator && !isParticipant) {
                throw new RuntimeException("공유 링크 접근 권한이 없습니다");
            }
            
            String shareUrl = baseUrl + "/routes/share/" + route.getShareCode();
            String qrCodeBase64 = QrCodeGenerator.generateQrCodeBase64(shareUrl);
            
            ShareLinkResponseDto response = ShareLinkResponseDto.of(
                    shareUrl,
                    route.getShareCode(),
                    qrCodeBase64
            );
            
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("공유 링크 생성 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    public ApiResponseDto<RouteResponseDto> getRouteByShareCode(String shareCode) {
        try {
            Route route = routeRepository.findByShareCode(shareCode)
                    .orElseThrow(() -> new RuntimeException("루트를 찾을 수 없습니다"));
            
            RouteResponseDto response = RouteResponseDto.from(route);
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("공유 코드로 루트 조회 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String trimmed = imageUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }

        String trimmed = memo.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 장소 이름으로 DB에서 첫 번째 사진 찾기
     */
    private String findFirstPlacePhoto(String placeName) {
        if (placeName == null || placeName.trim().isEmpty()) {
            return null;
        }
        
        try {
            return placeRepository.findByName(placeName.trim())
                    .flatMap(place -> {
                        List<PlacePhoto> photos = placePhotoRepository.findByPlaceId(place.getId());
                        if (photos != null && !photos.isEmpty()) {
                            return java.util.Optional.of(photos.get(0).getPhotoUrl());
                        }
                        return java.util.Optional.empty();
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("장소 사진 조회 실패: placeName={}", placeName, e);
            return null;
        }
    }

    private Comparator<Route> buildComparator(RouteSortType sortType, Map<Long, UserRoute> likedRouteMap) {
        RouteSortType effectiveSort = sortType != null ? sortType : RouteSortType.LATEST;

        return switch (effectiveSort) {
            case PINNED -> Comparator
                    .comparing((Route route) -> likedRouteMap.containsKey(route.getId()) ? 0 : 1)
                    .thenComparing(route -> {
                        UserRoute liked = likedRouteMap.get(route.getId());
                        if (liked != null) {
                            return liked.getUpdatedAt();
                        }
                        return route.getCreatedAt();
                    }, Comparator.nullsLast(Comparator.reverseOrder()));
            case OLDEST -> Comparator.comparing(Route::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case LATEST -> Comparator.comparing(Route::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private boolean hasRouteAccess(Route route, User user) {
        return route.getCreator().getId().equals(user.getId()) ||
                participantRepository.existsByRouteAndUser(route, user);
    }

    /**
     * HOT 루트 조회 (공개 루트만)
     */
    public List<HotRouteDto> getHotRoutes(int limit) {
        int fetchSize = Math.min(Math.max(limit, 1), 20);

        List<Object[]> ranking = userRouteRepository.findHotRoutes(fetchSize, HOT_RECENT_DAYS, HOT_ALPHA);
        log.info("HOT 루트 쿼리 결과: {} 개 (요청한 limit: {})", ranking.size(), fetchSize);
        if (ranking.isEmpty()) {
            log.warn("HOT 루트 없음 - 공개 루트가 없거나 좋아요가 없습니다");
            return List.of();
        }

        List<Long> routeIds = ranking.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());
        log.info("HOT 루트 ID 목록: {}", routeIds);

        Map<Long, Route> routeMap = routeRepository.findAllByIdWithCreator(routeIds).stream()
                .collect(Collectors.toMap(Route::getId, route -> route));
        log.info("조회된 루트 개수: {} / 요청한 ID 개수: {}", routeMap.size(), routeIds.size());

        List<HotRouteDto> hotRoutes = new ArrayList<>();
        int rank = 1;
        for (Object[] row : ranking) {
            Long routeId = ((Number) row[0]).longValue();
            Route route = routeMap.get(routeId);
            if (route == null) {
                log.warn("루트 ID {} 를 찾을 수 없습니다", routeId);
                continue;
            }

            Long totalLikes = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            Long recentLikes = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            
            log.info("HOT 루트 순위 {}: ID={}, 제목={}, 공개={}, 총좋아요={}, 최근좋아요={}", 
                    rank, routeId, route.getTitle(), route.isPublic(), totalLikes, recentLikes);

            hotRoutes.add(HotRouteDto.builder()
                    .rank(rank++)
                    .routeId(route.getId())
                    .title(route.getTitle())
                    .startDate(route.getStartDate())
                    .endDate(route.getEndDate())
                    .imageUrl(route.getImageUrl())
                    .creatorId(route.getCreator().getId())
                    .creatorNickname(route.getCreator().getNickname())
                    .totalLikes(totalLikes)
                    .recentLikes(recentLikes)
                    .build());

            if (hotRoutes.size() >= limit) {
                break;
            }
        }

        log.info("최종 반환할 HOT 루트 개수: {} (요청한 limit: {})", hotRoutes.size(), limit);
        return hotRoutes;
    }
}

