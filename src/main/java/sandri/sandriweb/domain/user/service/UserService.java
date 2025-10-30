package sandri.sandriweb.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.user.dto.*;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // 임시 인증번호 저장용 (실제로는 Redis 등을 사용하는 것이 좋습니다)
    private final Map<String, String> verificationCodes = new HashMap<>();
    
    // 임시 회원가입 1단계 정보 저장용 (실제로는 세션이나 Redis 사용)
    private final Map<String, RegisterStep1RequestDto> tempUserInfo = new HashMap<>();
    
    @Transactional
    public ApiResponseDto<LoginResponseDto> login(LoginRequestDto request) {
        try {
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("비밀번호가 일치하지 않습니다");
            }
            
            if (!user.isEnabled()) {
                throw new RuntimeException("비활성화된 계정입니다");
            }
            
            UserResponseDto userDto = UserResponseDto.from(user);
            LoginResponseDto loginResponse = LoginResponseDto.of(userDto);
            return ApiResponseDto.success("로그인 성공", loginResponse);
            
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<String> registerStep1(RegisterStep1RequestDto request) {
        try {
            // 중복 검사
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new RuntimeException("이미 등록된 휴대폰 번호입니다");
            }
            
            // 임시 저장 (실제로는 세션에 저장)
            tempUserInfo.put(request.getPhoneNumber(), request);
            
            log.info("회원가입 1단계 완료: {}", request.getPhoneNumber());
            return ApiResponseDto.success("회원가입 1단계가 완료되었습니다", request.getPhoneNumber());
            
        } catch (Exception e) {
            log.error("회원가입 1단계 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    public ApiResponseDto<String> sendVerificationCode(String phoneNumber) {
        try {
            // 실제로는 SMS 발송 서비스를 사용해야 합니다
            String verificationCode = generateVerificationCode();
            verificationCodes.put(phoneNumber, verificationCode);
            
            log.info("인증번호 발송: {} -> {}", phoneNumber, verificationCode);
            
            return ApiResponseDto.success("인증번호가 발송되었습니다", phoneNumber);
            
        } catch (Exception e) {
            log.error("인증번호 발송 실패: {}", e.getMessage());
            return ApiResponseDto.error("인증번호 발송에 실패했습니다");
        }
    }
    
    public ApiResponseDto<PhoneVerificationResponseDto> verifyPhone(PhoneVerificationRequestDto request) {
        try {
            String storedCode = verificationCodes.get(request.getPhoneNumber());
            
            if (storedCode == null) {
                throw new RuntimeException("인증번호가 만료되었습니다. 다시 요청해주세요");
            }
            
            if (!storedCode.equals(request.getVerificationCode())) {
                throw new RuntimeException("인증번호가 일치하지 않습니다");
            }
            
            // 인증 성공 시 저장된 코드 제거
            verificationCodes.remove(request.getPhoneNumber());
            
            PhoneVerificationResponseDto response = PhoneVerificationResponseDto.success(request.getPhoneNumber());
            return ApiResponseDto.success("휴대폰 인증이 완료되었습니다", response);
            
        } catch (Exception e) {
            log.error("휴대폰 인증 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<UserResponseDto> registerStep2(RegisterStep2RequestDto request, String phoneNumber) {
        try {
            // 비밀번호 확인
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("비밀번호가 일치하지 않습니다");
            }
            
            // 중복 검사
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("이미 사용 중인 아이디입니다");
            }
            
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new RuntimeException("이미 사용 중인 닉네임입니다");
            }
            
            // 1단계에서 입력한 정보 가져오기
            RegisterStep1RequestDto step1Info = tempUserInfo.get(phoneNumber);
            if (step1Info == null) {
                throw new RuntimeException("회원가입 1단계 정보를 찾을 수 없습니다. 다시 시작해주세요");
            }
            
            User user = User.builder()
                    .name(step1Info.getName())
                    .birthDate(step1Info.getBirthDate())
                    .gender(step1Info.getGender())
                    .telecomCarrier(step1Info.getTelecomCarrier())
                    .phoneNumber(step1Info.getPhoneNumber())
                    .nickname(request.getNickname())
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phoneVerified(true) // 휴대폰 인증이 완료되었다고 가정
                    //.enabled(true)
                    .build();
            
            User savedUser = userRepository.save(user);
            
            // 임시 정보 제거
            tempUserInfo.remove(phoneNumber);
            
            UserResponseDto userDto = UserResponseDto.from(savedUser);
            return ApiResponseDto.success("회원가입이 완료되었습니다", userDto);
            
        } catch (Exception e) {
            log.error("회원가입 2단계 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    public ApiResponseDto<UserResponseDto> getUserProfile(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            UserResponseDto userDto = UserResponseDto.from(user);
            return ApiResponseDto.success(userDto);
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}