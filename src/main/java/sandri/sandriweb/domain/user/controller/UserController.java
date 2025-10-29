package sandri.sandriweb.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.domain.user.dto.UserResponseDto;
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
}
