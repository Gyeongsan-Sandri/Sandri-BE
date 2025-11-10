package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.user.entity.User;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 응답 DTO")
public class UserResponseDto {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    
    @Schema(description = "이름", example = "홍길동")
    private String name;
    
    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;
    
    @Schema(description = "아이디", example = "hong123")
    private String username;
    
    @Schema(description = "생년월일", example = "1990-01-01")
    private LocalDate birthDate;
    
    @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private User.Gender gender;
    
    @Schema(description = "사는 곳", example = "경산시")
    private String location;
    
    @Schema(description = "위도", example = "35.8251")
    private Double latitude;
    
    @Schema(description = "경도", example = "128.7405")
    private Double longitude;
    
    @Schema(description = "추천인 아이디", example = "friend123")
    private String referrerUsername;
    
    @Schema(description = "계정 활성화 여부", example = "true")
    private boolean enabled;
    
    @Schema(
            description = "여행 스타일", 
            example = "ADVENTURER", 
            allowableValues = {
                "ADVENTURER", "SENSITIVE_FAIRY", "HOTSPOT_HUNTER", "LOCAL",
                "THOROUGH_PLANNER", "HEALING_TURTLE", "WALKER", "GALLERY_PEOPLE"
            }
    )
    private User.TravelStyle travelStyle;
    
    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .username(user.getUsername())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .location(user.getLocation())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .referrerUsername(user.getReferrerUsername())
                .enabled(user.isEnabled())
                .travelStyle(user.getTravelStyle())
                .build();
    }
}
