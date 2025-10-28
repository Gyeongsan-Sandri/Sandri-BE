package sandri.sandriweb.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.user.dto.*;
import sandri.sandriweb.domain.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "인증", description = "로그인, 회원가입, 휴대폰 인증 API")
public class AuthController {
    
    private final UserService userService;
    
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 아이디와 비밀번호로 로그인합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {
        
        log.info("로그인 시도: {}", request.getUsername());
        ApiResponseDto<LoginResponseDto> response = userService.login(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/register/step1")
    @Operation(summary = "회원가입 1단계", description = "개인정보 입력 및 휴대폰 번호 등록")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "1단계 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<String>> registerStep1(
            @Valid @RequestBody RegisterStep1RequestDto request) {
        
        log.info("회원가입 1단계: {}", request.getPhoneNumber());
        ApiResponseDto<String> response = userService.registerStep1(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/verification/send")
    @Operation(summary = "인증번호 발송", description = "휴대폰 번호로 인증번호를 발송합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<String>> sendVerificationCode(
            @Parameter(description = "휴대폰 번호", example = "010-1234-5678")
            @RequestParam String phoneNumber) {
        
        log.info("인증번호 발송 요청: {}", phoneNumber);
        ApiResponseDto<String> response = userService.sendVerificationCode(phoneNumber);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/verification/verify")
    @Operation(summary = "휴대폰 인증", description = "발송된 인증번호로 휴대폰을 인증합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<PhoneVerificationResponseDto>> verifyPhone(
            @Valid @RequestBody PhoneVerificationRequestDto request) {
        
        log.info("휴대폰 인증 시도: {}", request.getPhoneNumber());
        ApiResponseDto<PhoneVerificationResponseDto> response = userService.verifyPhone(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/register/step2")
    @Operation(summary = "회원가입 2단계", description = "계정 정보 입력 및 회원가입 완료")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> registerStep2(
            @Valid @RequestBody RegisterStep2RequestDto request,
            @Parameter(description = "휴대폰 번호", example = "010-1234-5678")
            @RequestParam String phoneNumber) {
        
        log.info("회원가입 2단계: {}", request.getUsername());
        ApiResponseDto<UserResponseDto> response = userService.registerStep2(request, phoneNumber);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
