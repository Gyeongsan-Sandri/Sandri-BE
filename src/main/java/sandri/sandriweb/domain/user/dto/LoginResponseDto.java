package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO")
public class LoginResponseDto {
    
    @Schema(description = "사용자 정보")
    private UserResponseDto user;
    
    public static LoginResponseDto of(UserResponseDto user) {
        return LoginResponseDto.builder().user(user).build();
    }
}


