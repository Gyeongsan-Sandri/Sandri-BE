package sandri.sandriweb.domain.user.dto;

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
public class UserResponseDto {
    
    private Long id;
    private String name;
    private String nickname;
    private String username;
    private LocalDate birthDate;
    private User.Gender gender;
    private String location;
    private String referrerUsername;
    private boolean enabled;
    
    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .username(user.getUsername())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .location(user.getLocation())
                .referrerUsername(user.getReferrerUsername())
                .enabled(user.isEnabled())
                .build();
    }
}
