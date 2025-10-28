package sandri.sandriweb.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    
    private UserResponseDto user;
    private String message;
    
    public static LoginResponseDto of(UserResponseDto user) {
        return LoginResponseDto.builder()
                .user(user)
                .message("로그인 성공")
                .build();
    }
}
