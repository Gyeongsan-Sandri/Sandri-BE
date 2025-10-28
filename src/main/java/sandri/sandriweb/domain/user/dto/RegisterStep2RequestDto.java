package sandri.sandriweb.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStep2RequestDto {
    
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 30, message = "닉네임은 2-30자 사이여야 합니다")
    private String nickname;
    
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 30, message = "아이디는 4-30자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용 가능합니다")
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).*$", 
             message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;
    
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;
}
