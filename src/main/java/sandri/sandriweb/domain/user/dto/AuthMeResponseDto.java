package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.user.entity.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 확인 응답 DTO")
public class AuthMeResponseDto {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    
    @Schema(description = "아이디", example = "hong123")
    private String username;
    
    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;
    
    public static AuthMeResponseDto from(User user) {
        return AuthMeResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }
}

