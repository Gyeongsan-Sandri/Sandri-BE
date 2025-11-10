package sandri.sandriweb.domain.admin.dto;

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
@Schema(description = "태그 생성 요청 DTO")
public class CreateTagRequestDto {

    @NotBlank(message = "태그 이름은 필수입니다")
    @Schema(description = "태그 이름", example = "사진명소", required = true)
    private String name;
}

