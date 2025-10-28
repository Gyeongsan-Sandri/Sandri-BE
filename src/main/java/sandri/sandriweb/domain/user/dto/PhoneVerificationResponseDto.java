package sandri.sandriweb.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneVerificationResponseDto {
    
    private String phoneNumber;
    private boolean verified;
    private String message;
    
    public static PhoneVerificationResponseDto success(String phoneNumber) {
        return PhoneVerificationResponseDto.builder()
                .phoneNumber(phoneNumber)
                .verified(true)
                .message("휴대폰 인증이 완료되었습니다")
                .build();
    }
    
    public static PhoneVerificationResponseDto error(String message) {
        return PhoneVerificationResponseDto.builder()
                .verified(false)
                .message(message)
                .build();
    }
}
