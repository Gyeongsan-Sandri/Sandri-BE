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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.user.dto.*;
import sandri.sandriweb.domain.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "인증", description = "회원가입/로그인 API")
public class AuthController {
    
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    
    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "아이디와 비밀번호로 로그인합니다. 로그인 성공 시 세션 쿠키가 자동으로 설정됩니다.",
            requestBody = @RequestBody(
                    description = "로그인 요청 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequestDto.class),
                            examples = @ExampleObject(
                                    name = "로그인 예제",
                                    value = "{\n  \"username\": \"hong123\",\n  \"password\": \"password123!\"\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\n  \"success\": true,\n  \"message\": \"로그인 성공\",\n  \"data\": {\n    \"user\": {\n      \"id\": 1,\n      \"name\": \"홍길동\",\n      \"nickname\": \"홍길동\",\n      \"username\": \"hong123\",\n      \"birthDate\": \"1990-01-01\",\n      \"gender\": \"MALE\",\n      \"location\": \"경산시\",\n      \"enabled\": true\n    }\n  }\n}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        
        log.info("로그인 시도: {}", request.getUsername());
        
        try {
            // Spring Security 인증 수행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            
            // SecurityContext 생성 및 인증 정보 설정
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            
            // 세션 생성 및 SecurityContext 저장
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
            
            log.info("세션 생성 완료: sessionId={}", session.getId());
            
            // UserService를 통해 응답 생성
            ApiResponseDto<LoginResponseDto> response = userService.login(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("로그인에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/register")
    @Operation(
            summary = "회원가입",
            description = "사용자 정보를 입력하여 회원가입합니다. 아이디와 닉네임은 중복될 수 없습니다.",
            requestBody = @RequestBody(
                    description = "회원가입 요청 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequestDto.class),
                            examples = @ExampleObject(
                                    name = "회원가입 예제",
                                    value = "{\n  \"name\": \"홍길동\",\n  \"username\": \"hong123\",\n  \"password\": \"password123!\",\n  \"confirmPassword\": \"password123!\",\n  \"nickname\": \"홍길동\",\n  \"birthDate\": \"1990-01-01\",\n  \"gender\": \"MALE\",\n  \"location\": \"경산시\",\n  \"referrerUsername\": \"friend123\"\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 완료",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\n  \"success\": true,\n  \"message\": \"회원가입이 완료되었습니다\",\n  \"data\": {\n    \"id\": 1,\n    \"name\": \"홍길동\",\n    \"nickname\": \"홍길동\",\n    \"username\": \"hong123\",\n    \"birthDate\": \"1990-01-01\",\n    \"gender\": \"MALE\",\n    \"location\": \"경산시\",\n    \"enabled\": true\n  }\n}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복된 아이디/닉네임, 유효성 검증 실패)")
    })
    public ResponseEntity<ApiResponseDto<UserResponseDto>> register(
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequestDto request) {
        
        log.info("회원가입 시도: {}", request.getNickname());
        ApiResponseDto<UserResponseDto> response = userService.register(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/check-username")
    @Operation(
            summary = "아이디 중복 확인",
            description = "회원가입 전 아이디 중복 여부를 확인합니다. 사용 가능한 아이디인지 확인할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "중복 확인 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "사용 가능",
                                            value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"isDuplicate\": false,\n    \"message\": \"사용 가능한 아이디입니다\"\n  }\n}"
                                    ),
                                    @ExampleObject(
                                            name = "중복됨",
                                            value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"isDuplicate\": true,\n    \"message\": \"이미 사용 중인 아이디입니다\"\n  }\n}"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<CheckDuplicateResponseDto>> checkUsername(
            @Parameter(description = "확인할 아이디", example = "hong123", required = true)
            @RequestParam String username) {
        
        log.info("아이디 중복 확인 요청: username={}", username);
        ApiResponseDto<CheckDuplicateResponseDto> response = userService.checkUsernameDuplicate(username);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-nickname")
    @Operation(
            summary = "닉네임 중복 확인",
            description = "회원가입 전 닉네임 중복 여부를 확인합니다. 사용 가능한 닉네임인지 확인할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "중복 확인 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "사용 가능",
                                            value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"isDuplicate\": false,\n    \"message\": \"사용 가능한 닉네임입니다\"\n  }\n}"
                                    ),
                                    @ExampleObject(
                                            name = "중복됨",
                                            value = "{\n  \"success\": true,\n  \"message\": \"성공\",\n  \"data\": {\n    \"isDuplicate\": true,\n    \"message\": \"이미 사용 중인 닉네임입니다\"\n  }\n}"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<CheckDuplicateResponseDto>> checkNickname(
            @Parameter(description = "확인할 닉네임", example = "홍길동", required = true)
            @RequestParam String nickname) {
        
        log.info("닉네임 중복 확인 요청: nickname={}", nickname);
        ApiResponseDto<CheckDuplicateResponseDto> response = userService.checkNicknameDuplicate(nickname);
        
        return ResponseEntity.ok(response);
    }
}
