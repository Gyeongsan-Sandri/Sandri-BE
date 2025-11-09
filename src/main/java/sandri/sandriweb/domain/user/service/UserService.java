package sandri.sandriweb.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.repository.PlacePhotoRepository;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.review.repository.PlaceReviewRepository;
import sandri.sandriweb.domain.user.dto.*;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.entity.UserVisitedPlace;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.domain.user.repository.UserVisitedPlaceRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserVisitedPlaceRepository userVisitedPlaceRepository;
    private final PlaceRepository placeRepository;
    private final PlacePhotoRepository placePhotoRepository;
    private final PlaceReviewRepository placeReviewRepository;
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
    
    @Transactional
    public ApiResponseDto<UserResponseDto> updateLocation(String username, UpdateLocationRequestDto request) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            user.updateLocation(request.getLatitude(), request.getLongitude());
            User savedUser = userRepository.save(user);
            
            UserResponseDto userDto = UserResponseDto.from(savedUser);
            return ApiResponseDto.success("위치 정보가 업데이트되었습니다", userDto);
            
        } catch (Exception e) {
            log.error("위치 정보 업데이트 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<VisitedPlaceResponseDto> saveVisitedPlace(String username, SaveVisitedPlaceRequestDto request) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            Place place = placeRepository.findById(request.getPlaceId())
                    .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다"));
            
            // 중복 저장 방지: 같은 사용자가 같은 장소를 이미 저장했는지 확인
            userVisitedPlaceRepository.findByUserIdAndPlaceId(user.getId(), place.getId())
                    .ifPresent(existing -> {
                        throw new RuntimeException("이미 방문한 장소입니다");
                    });
            
            UserVisitedPlace userVisitedPlace = UserVisitedPlace.builder()
                    .user(user)
                    .place(place)
                    .visitDate(request.getVisitDate())
                    .build();
            
            UserVisitedPlace saved = userVisitedPlaceRepository.save(userVisitedPlace);
            
            // 리뷰 작성 여부 확인
            boolean hasReview = placeReviewRepository.findByPlaceIdOrderByCreatedAtDesc(place.getId()).stream()
                    .anyMatch(review -> review.getUser().getId().equals(user.getId()) && review.isEnabled());
            
            // 썸네일 URL 조회 (첫 번째 사진)
            String thumbnailUrl = placePhotoRepository.findByPlaceId(place.getId()).stream()
                    .findFirst()
                    .map(photo -> photo.getPhotoUrl())
                    .orElse(null);
            
            VisitedPlaceResponseDto responseDto = VisitedPlaceResponseDto.from(saved, thumbnailUrl, hasReview);
            return ApiResponseDto.success("방문 장소가 저장되었습니다", responseDto);
            
        } catch (Exception e) {
            log.error("방문 장소 저장 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    public ApiResponseDto<List<VisitedPlaceResponseDto>> getMyVisitedPlaces(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            List<UserVisitedPlace> visitedPlaces = userVisitedPlaceRepository.findByUserIdOrderByVisitDateDesc(user.getId());
            
            // 장소 ID 목록 추출
            List<Long> placeIds = visitedPlaces.stream()
                    .map(uvp -> uvp.getPlace().getId())
                    .collect(Collectors.toList());
            
            // 배치로 썸네일 URL 조회 (N+1 문제 방지)
            Map<Long, String> thumbnailMap = placePhotoRepository.findFirstPhotoUrlByPlaceIdIn(placeIds).stream()
                    .collect(Collectors.toMap(
                            result -> ((Number) result[0]).longValue(),
                            result -> (String) result[1]
                    ));
            
            // 각 방문 장소의 리뷰 작성 여부 확인
            List<VisitedPlaceResponseDto> responseDtos = visitedPlaces.stream()
                    .map(uvp -> {
                        // 각 장소별로 리뷰 확인
                        boolean hasReview = placeReviewRepository.findByPlaceIdOrderByCreatedAtDesc(uvp.getPlace().getId()).stream()
                                .anyMatch(review -> review.getUser().getId().equals(user.getId()) && review.isEnabled());
                        String thumbnailUrl = thumbnailMap.get(uvp.getPlace().getId());
                        return VisitedPlaceResponseDto.from(uvp, thumbnailUrl, hasReview);
                    })
                    .collect(Collectors.toList());
            
            return ApiResponseDto.success(responseDtos);
            
        } catch (Exception e) {
            log.error("방문 장소 목록 조회 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    @Transactional
    public ApiResponseDto<Void> deleteVisitedPlace(String username, Long placeId) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            UserVisitedPlace userVisitedPlace = userVisitedPlaceRepository.findByUserIdAndPlaceId(user.getId(), placeId)
                    .orElseThrow(() -> new RuntimeException("방문 기록을 찾을 수 없습니다"));
            
            userVisitedPlace.disable();
            userVisitedPlaceRepository.save(userVisitedPlace);
            
            return ApiResponseDto.success("방문 기록이 삭제되었습니다", null);
            
        } catch (Exception e) {
            log.error("방문 기록 삭제 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
    
    /**
     * 아이디 중복 확인
     */
    public ApiResponseDto<CheckDuplicateResponseDto> checkUsernameDuplicate(String username) {
        try {
            boolean isDuplicate = userRepository.existsByUsername(username);
            
            CheckDuplicateResponseDto response = CheckDuplicateResponseDto.builder()
                    .isDuplicate(isDuplicate)
                    .message(isDuplicate ? "이미 사용 중인 아이디입니다" : "사용 가능한 아이디입니다")
                    .build();
            
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("아이디 중복 확인 실패: {}", e.getMessage());
            return ApiResponseDto.error("중복 확인 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 닉네임 중복 확인
     */
    public ApiResponseDto<CheckDuplicateResponseDto> checkNicknameDuplicate(String nickname) {
        try {
            boolean isDuplicate = userRepository.existsByNickname(nickname);
            
            CheckDuplicateResponseDto response = CheckDuplicateResponseDto.builder()
                    .isDuplicate(isDuplicate)
                    .message(isDuplicate ? "이미 사용 중인 닉네임입니다" : "사용 가능한 닉네임입니다")
                    .build();
            
            return ApiResponseDto.success(response);
            
        } catch (Exception e) {
            log.error("닉네임 중복 확인 실패: {}", e.getMessage());
            return ApiResponseDto.error("중복 확인 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 닉네임 수정
     */
    @Transactional
    public ApiResponseDto<UserResponseDto> updateNickname(String username, UpdateNicknameRequestDto request) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            // 현재 닉네임과 동일한지 확인
            if (user.getNickname().equals(request.getNickname())) {
                throw new RuntimeException("현재 닉네임과 동일합니다");
            }
            
            // 닉네임 중복 확인
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new RuntimeException("이미 사용 중인 닉네임입니다");
            }
            
            user.updateNickname(request.getNickname());
            User savedUser = userRepository.save(user);
            
            UserResponseDto userDto = UserResponseDto.from(savedUser);
            return ApiResponseDto.success("닉네임이 수정되었습니다", userDto);
            
        } catch (Exception e) {
            log.error("닉네임 수정 실패: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        }
    }
}