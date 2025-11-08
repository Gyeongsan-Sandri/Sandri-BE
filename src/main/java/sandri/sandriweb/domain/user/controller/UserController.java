package sandri.sandriweb.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.user.dto.*;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.domain.user.service.UserService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "사용자", description = "사용자 정보 관련 API")
public class UserController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    
    @GetMapping("/profile")
    @Operation(summary = "사용자 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getUserProfile(Authentication authentication) {
        
        String username = authentication.getName();
        log.info("사용자 프로필 조회: {}", username);
        
        // username으로 User 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        UserResponseDto userDto = UserResponseDto.from(user);
        ApiResponseDto<UserResponseDto> response = ApiResponseDto.success(userDto);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profile/{nickname}")
    @Operation(
            summary = "닉네임으로 프로필 조회",
            description = "닉네임으로 사용자 프로필 정보를 조회합니다. 인증이 필요하지 않습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"id\": 1,\n    \"name\": \"홍길동\",\n    \"nickname\": \"홍길동\",\n    \"username\": \"hong123\",\n    \"birthDate\": \"1990-01-01\",\n    \"gender\": \"MALE\",\n    \"location\": \"경산시\",\n    \"enabled\": true\n  }\n}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getUserProfileByNickname(
            @Parameter(description = "사용자 닉네임", example = "홍길동", required = true)
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
    @Operation(
            summary = "여행 스타일 저장",
            description = "현재 로그인한 사용자의 여행 스타일을 저장합니다. 가능한 값: ADVENTURE, RELAXATION, CULTURE, NATURE, FOOD, SHOPPING",
            requestBody = @RequestBody(
                    description = "여행 스타일 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SaveTravelStyleRequestDto.class),
                            examples = @ExampleObject(
                                    name = "여행 스타일 저장 예제",
                                    value = "{\n  \"travelStyle\": \"ADVENTURE\"\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> saveTravelStyle(
            @Valid @org.springframework.web.bind.annotation.RequestBody SaveTravelStyleRequestDto request,
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
    @Operation(
            summary = "위치 정보 업데이트",
            description = "현재 로그인한 사용자의 GPS 위치 정보(위도/경도)를 업데이트합니다. 위도는 -90.0 ~ 90.0, 경도는 -180.0 ~ 180.0 범위여야 합니다.",
            requestBody = @RequestBody(
                    description = "위치 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateLocationRequestDto.class),
                            examples = @ExampleObject(
                                    name = "위치 업데이트 예제",
                                    value = "{\n  \"latitude\": 35.8251,\n  \"longitude\": 128.7405\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (범위 초과)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> updateLocation(
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateLocationRequestDto request,
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
    @Operation(
            summary = "방문 장소 저장",
            description = "현재 로그인한 사용자가 방문한 장소를 저장합니다. 같은 장소를 중복 저장할 수 없습니다.",
            requestBody = @RequestBody(
                    description = "방문 장소 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SaveVisitedPlaceRequestDto.class),
                            examples = @ExampleObject(
                                    name = "방문 장소 저장 예제",
                                    value = "{\n  \"placeId\": 1,\n  \"visitDate\": \"2024-11-05\"\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\n  \"success\": true,\n  \"message\": \"방문 장소가 저장되었습니다\",\n  \"data\": {\n    \"userVisitedPlaceId\": 1,\n    \"placeId\": 1,\n    \"placeName\": \"경주 불국사\",\n    \"placeAddress\": \"경상북도 경주시 불국로 385\",\n    \"placeThumbnailUrl\": \"https://s3.../photo.jpg\",\n    \"visitDate\": \"2024-11-05\",\n    \"hasReview\": false\n  }\n}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 방문한 장소, 장소 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<VisitedPlaceResponseDto>> saveVisitedPlace(
            @Valid @org.springframework.web.bind.annotation.RequestBody SaveVisitedPlaceRequestDto request,
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
    @Operation(
            summary = "방문 기록 삭제",
            description = "현재 로그인한 사용자의 방문 기록을 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (방문 기록 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteVisitedPlace(
            @Parameter(description = "장소 ID", example = "1", required = true)
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
