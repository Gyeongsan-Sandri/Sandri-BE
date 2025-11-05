package sandri.sandriweb.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.user.dto.*;
import sandri.sandriweb.domain.user.service.UserService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "사용자", description = "사용자 정보 관련 API")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    @Operation(summary = "사용자 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getUserProfile(Authentication authentication) {
        
        String nickname = authentication.getName();
        log.info("사용자 프로필 조회: {}", nickname);
        ApiResponseDto<UserResponseDto> response = userService.getUserProfile(nickname);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/profile/{nickname}")
    @Operation(summary = "닉네임으로 프로필 조회", description = "닉네임으로 사용자 프로필 정보를 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getUserProfileByNickname(
            @PathVariable String nickname) {
        
        log.info("사용자 프로필 조회 (닉네임): {}", nickname);
        ApiResponseDto<UserResponseDto> response = userService.getUserProfile(nickname);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/travel-style")
    @Operation(summary = "여행 스타일 저장", description = "현재 로그인한 사용자의 여행 스타일을 저장합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> saveTravelStyle(
            @Valid @RequestBody SaveTravelStyleRequestDto request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("여행 스타일 저장 요청: 사용자={}, 여행스타일={}", username, request.getTravelStyle());
        ApiResponseDto<UserResponseDto> response = userService.saveTravelStyle(username, request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PutMapping("/location")
    @Operation(summary = "위치 정보 업데이트", description = "현재 로그인한 사용자의 GPS 위치 정보(위도/경도)를 업데이트합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> updateLocation(
            @Valid @RequestBody UpdateLocationRequestDto request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("위치 정보 업데이트 요청: 사용자={}, 위도={}, 경도={}", username, request.getLatitude(), request.getLongitude());
        ApiResponseDto<UserResponseDto> response = userService.updateLocation(username, request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/visited-places")
    @Operation(summary = "방문 장소 저장", description = "현재 로그인한 사용자가 방문한 장소를 저장합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<VisitedPlaceResponseDto>> saveVisitedPlace(
            @Valid @RequestBody SaveVisitedPlaceRequestDto request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("방문 장소 저장 요청: 사용자={}, 장소ID={}, 방문날짜={}", username, request.getPlaceId(), request.getVisitDate());
        ApiResponseDto<VisitedPlaceResponseDto> response = userService.saveVisitedPlace(username, request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/visited-places")
    @Operation(summary = "내 여행 목록 조회", description = "현재 로그인한 사용자가 방문한 장소 목록을 조회합니다 (방문 날짜 최신순)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<java.util.List<VisitedPlaceResponseDto>>> getMyVisitedPlaces(
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("내 여행 목록 조회 요청: 사용자={}", username);
        ApiResponseDto<java.util.List<VisitedPlaceResponseDto>> response = userService.getMyVisitedPlaces(username);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/visited-places/{placeId}")
    @Operation(summary = "방문 기록 삭제", description = "현재 로그인한 사용자의 방문 기록을 삭제합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteVisitedPlace(
            @PathVariable Long placeId,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("방문 기록 삭제 요청: 사용자={}, 장소ID={}", username, placeId);
        ApiResponseDto<Void> response = userService.deleteVisitedPlace(username, placeId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
