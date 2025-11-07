package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "중복 확인 요청 DTO")
public class CheckDuplicateRequestDto {
    
    @NotBlank(message = "확인할 값은 필수입니다")
    @Schema(description = "확인할 아이디 또는 닉네임", example = "hong123", required = true)
    private String value;
}

