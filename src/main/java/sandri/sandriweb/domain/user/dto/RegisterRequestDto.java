package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
@Schema(description = "회원가입 요청 DTO")
public class RegisterRequestDto {
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    @Schema(description = "이름", example = "홍길동")
    private String name;

    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 30, message = "아이디는 4-30자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용 가능합니다")
    @Schema(description = "아이디", example = "hong123")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).*$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
    @Schema(description = "비밀번호", example = "password123!")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    @Schema(description = "비밀번호 확인", example = "password123!")
    private String confirmPassword;

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 30, message = "닉네임은 2-30자 사이여야 합니다")
    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @NotNull(message = "생년월일은 필수입니다")
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    @Schema(description = "생년월일", example = "1990-01-01")
    private LocalDate birthDate;

    @NotNull(message = "성별은 필수입니다")
    @Schema(description = "성별", example = "MALE")
    private User.Gender gender;

    @NotBlank(message = "사는 곳은 필수입니다")
    @Size(max = 100, message = "사는 곳은 100자 이하여야 합니다")
    @Schema(description = "사는 곳", example = "경산시")
    private String location;

    @Size(max = 30, message = "추천인 아이디는 30자 이하여야 합니다")
    @Schema(description = "추천인 아이디", example = "friend123", required = false)
    private String referrerUsername;
}


