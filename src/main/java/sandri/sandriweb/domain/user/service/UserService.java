package sandri.sandriweb.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.user.dto.*;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
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
    public ApiResponseDto<UserResponseDto> register(RegisterRequestDto request) {
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
            
            User user = User.builder()
                    .name(request.getName())
                    .nickname(request.getNickname())
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .birthDate(request.getBirthDate())
                    .gender(request.getGender())
                    .location(request.getLocation())
                    .referrerUsername(request.getReferrerUsername())
                    .enabled(true)
                    .build();
            
            User savedUser = userRepository.save(user);
            
            UserResponseDto userDto = UserResponseDto.from(savedUser);
            return ApiResponseDto.success("회원가입이 완료되었습니다", userDto);
            
        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    public ApiResponseDto<UserResponseDto> getUserProfile(String nickname) {
        try {
            User user = userRepository.findByNickname(nickname)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            UserResponseDto userDto = UserResponseDto.from(user);
            return ApiResponseDto.success(userDto);
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<UserResponseDto> saveTravelStyle(String username, SaveTravelStyleRequestDto request) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            user.updateTravelStyle(request.getTravelStyle());
            User savedUser = userRepository.save(user);
            
            UserResponseDto userDto = UserResponseDto.from(savedUser);
            return ApiResponseDto.success("여행 스타일이 저장되었습니다", userDto);
            
        } catch (Exception e) {
            log.error("여행 스타일 저장 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
}