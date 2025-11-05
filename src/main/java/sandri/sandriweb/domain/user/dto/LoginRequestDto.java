package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 요청 DTO")
public class LoginRequestDto {
    
    @NotBlank(message = "아이디는 필수입니다")
    @Size(max = 30, message = "아이디는 30자 이하여야 합니다")
    @Schema(description = "사용자 아이디", example = "hong123", required = true)
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    @Schema(description = "비밀번호", example = "password123!", required = true)
    private String password;
}


