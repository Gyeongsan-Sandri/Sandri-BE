package sandri.sandriweb.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.user.entity.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    
    private Long id;
    private String name;
    private String nickname;
    private String username;
    private String phoneNumber;
    private User.Gender gender;
    private User.TelecomCarrier telecomCarrier;
    private boolean phoneVerified;
    private boolean enabled;
    
    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .telecomCarrier(user.getTelecomCarrier())
                .phoneVerified(user.isPhoneVerified())
                .enabled(user.isEnabled())
                .build();
    }
}
