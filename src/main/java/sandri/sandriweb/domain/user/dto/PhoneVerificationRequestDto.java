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
public class PhoneVerificationRequestDto {
    
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]-[0-9]{4}-[0-9]{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
    private String phoneNumber;
    
    @NotBlank(message = "인증번호는 필수입니다")
    @Size(min = 6, max = 6, message = "인증번호는 6자리여야 합니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 숫자 6자리여야 합니다")
    private String verificationCode;
}
