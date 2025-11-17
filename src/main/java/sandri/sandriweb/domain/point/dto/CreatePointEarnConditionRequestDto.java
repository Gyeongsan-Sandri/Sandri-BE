package sandri.sandriweb.domain.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.point.enums.ConditionType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포인트 적립 조건 생성/수정 요청 DTO")
public class CreatePointEarnConditionRequestDto {

    @NotNull(message = "적립 조건 타입은 필수입니다")
    @Schema(description = "적립 조건 타입", example = "SIGN_UP", required = true)
    private ConditionType conditionType;

    @NotNull(message = "적립 포인트 양은 필수입니다")
    @Min(value = 0, message = "적립 포인트는 0 이상이어야 합니다")
    @Schema(description = "적립 포인트 양", example = "100", required = true)
    private Long pointAmount;
}
