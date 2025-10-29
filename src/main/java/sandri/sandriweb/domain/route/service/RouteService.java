package sandri.sandriweb.domain.route.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.route.dto.*;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.entity.RouteLocation;
import sandri.sandriweb.domain.route.entity.RouteParticipant;
import sandri.sandriweb.domain.route.repository.RouteParticipantRepository;
import sandri.sandriweb.domain.route.repository.RouteRepository;
import sandri.sandriweb.domain.route.util.QrCodeGenerator;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RouteService {
    
    private final RouteRepository routeRepository;
    private final RouteParticipantRepository participantRepository;
    private final UserRepository userRepository;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Transactional
    public ApiResponseDto<RouteResponseDto> createRoute(CreateRouteRequestDto request, User creator) {
        try {
            Route route = Route.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .creator(creator)
                    .isPublic(request.isPublic())
                    .build();
            
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
            if (request.getDescription() != null) {
                route.updateDescription(request.getDescription());
            }
            if (request.getStartDate() != null && request.getEndDate() != null) {
                route.updateDates(request.getStartDate(), request.getEndDate());
            }
            if (request.getIsPublic() != null) {
                route.updateVisibility(request.getIsPublic());
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
    
    public ApiResponseDto<List<RouteResponseDto>> getUserRoutes(User user) {
        try {
            List<Route> routes = routeRepository.findByParticipantOrCreator(user);
            List<RouteResponseDto> response = routes.stream()
                    .map(RouteResponseDto::from)
                    .collect(Collectors.toList());
            
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("사용자 루트 조회 실패: {}", e.getMessage(), e);
            return ApiResponseDto.error(e.getMessage());
        }
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
            
            User participantUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
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
}

