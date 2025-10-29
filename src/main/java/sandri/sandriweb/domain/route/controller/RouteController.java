package sandri.sandriweb.domain.route.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.route.dto.*;
import sandri.sandriweb.domain.route.service.GoogleMapsService;
import sandri.sandriweb.domain.route.service.RouteService;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "루트", description = "여행 루트 관리 API")
public class RouteController {
    
    private final RouteService routeService;
    private final UserRepository userRepository;
    private final GoogleMapsService googleMapsService;
    
    @PostMapping
    @Operation(summary = "루트 생성", description = "새로운 여행 루트를 생성합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<RouteResponseDto>> createRoute(
            @Valid @RequestBody CreateRouteRequestDto request,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("루트 생성 요청: 사용자={}, 제목={}", username, request.getTitle());
        ApiResponseDto<RouteResponseDto> response = routeService.createRoute(request, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{routeId}")
    @Operation(summary = "루트 조회", description = "특정 루트의 상세 정보를 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "루트 없음")
    })
    public ResponseEntity<ApiResponseDto<RouteResponseDto>> getRoute(
            @PathVariable Long routeId,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("루트 조회 요청: 루트ID={}, 사용자={}", routeId, username);
        ApiResponseDto<RouteResponseDto> response = routeService.getRoute(routeId, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            if (response.getMessage().contains("권한")) {
                return ResponseEntity.status(403).body(response);
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{routeId}")
    @Operation(summary = "루트 수정", description = "기존 루트를 수정합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "루트 없음")
    })
    public ResponseEntity<ApiResponseDto<RouteResponseDto>> updateRoute(
            @PathVariable Long routeId,
            @Valid @RequestBody UpdateRouteRequestDto request,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("루트 수정 요청: 루트ID={}, 사용자={}", routeId, username);
        ApiResponseDto<RouteResponseDto> response = routeService.updateRoute(routeId, request, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            if (response.getMessage().contains("권한")) {
                return ResponseEntity.status(403).body(response);
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{routeId}")
    @Operation(summary = "루트 삭제", description = "루트를 삭제합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "루트 없음")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteRoute(
            @PathVariable Long routeId,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("루트 삭제 요청: 루트ID={}, 사용자={}", routeId, username);
        ApiResponseDto<Void> response = routeService.deleteRoute(routeId, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            if (response.getMessage().contains("권한")) {
                return ResponseEntity.status(403).body(response);
            }
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/my")
    @Operation(summary = "내 루트 목록 조회", description = "현재 사용자의 모든 루트 목록을 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<List<RouteResponseDto>>> getMyRoutes(Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("내 루트 목록 조회: 사용자={}", username);
        ApiResponseDto<List<RouteResponseDto>> response = routeService.getUserRoutes(user);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{routeId}/participants")
    @Operation(summary = "일행 추가", description = "루트에 일행을 추가합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponseDto<RouteResponseDto>> addParticipant(
            @PathVariable Long routeId,
            @Valid @RequestBody AddParticipantRequestDto request,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("일행 추가 요청: 루트ID={}, 사용자={}, 추가할사용자ID={}", routeId, username, request.getUserId());
        ApiResponseDto<RouteResponseDto> response = routeService.addParticipant(routeId, request, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{routeId}/participants")
    @Operation(summary = "일행 목록 조회", description = "루트의 일행 목록을 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    public ResponseEntity<ApiResponseDto<List<RouteResponseDto.ParticipantDto>>> getParticipants(
            @PathVariable Long routeId,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("일행 목록 조회: 루트ID={}, 사용자={}", routeId, username);
        ApiResponseDto<List<RouteResponseDto.ParticipantDto>> response = routeService.getParticipants(routeId, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(403).body(response);
        }
    }
    
    @DeleteMapping("/{routeId}/participants")
    @Operation(summary = "일행 삭제", description = "루트에서 일행을 삭제합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteParticipants(
            @PathVariable Long routeId,
            @RequestParam List<Long> participantIds,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("일행 삭제 요청: 루트ID={}, 사용자={}, 삭제할일행IDs={}", routeId, username, participantIds);
        ApiResponseDto<Void> response = routeService.deleteParticipants(routeId, participantIds, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(403).body(response);
        }
    }
    
    @GetMapping("/{routeId}/share")
    @Operation(summary = "공유 링크 생성", description = "루트의 공유 링크와 QR 코드를 생성합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    public ResponseEntity<ApiResponseDto<ShareLinkResponseDto>> getShareLink(
            @PathVariable Long routeId,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        log.info("공유 링크 생성 요청: 루트ID={}, 사용자={}", routeId, username);
        ApiResponseDto<ShareLinkResponseDto> response = routeService.getShareLink(routeId, user);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(403).body(response);
        }
    }
    
    @GetMapping("/share/{shareCode}")
    @Operation(summary = "공유 코드로 루트 조회", description = "공유 코드를 통해 루트를 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "루트 없음")
    })
    public ResponseEntity<ApiResponseDto<RouteResponseDto>> getRouteByShareCode(
            @PathVariable String shareCode) {
        
        log.info("공유 코드로 루트 조회: shareCode={}", shareCode);
        ApiResponseDto<RouteResponseDto> response = routeService.getRouteByShareCode(shareCode);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/places/search")
    @Operation(summary = "장소 검색", description = "Google Maps API를 사용하여 장소를 검색합니다. API 키가 없으면 더미 데이터를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<PlaceSearchResponseDto>> searchPlaces(
            @RequestParam String query) {
        
        log.info("장소 검색 요청: query={}", query);
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("검색어를 입력해주세요."));
        }
        
        PlaceSearchResponseDto result = googleMapsService.searchPlacesDto(query.trim());
        return ResponseEntity.ok(ApiResponseDto.success(result));
    }
}

