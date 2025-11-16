package sandri.sandriweb.domain.magazine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Schema(description = "매거진 카드 정보")
public class MagazineCardInfoDto {
    
    @NotNull(message = "카드 순서는 필수입니다")
    @Min(value = 0, message = "카드 순서는 0 이상이어야 합니다")
    @Schema(description = "카드 순서 (0부터 시작)", example = "0", required = true)
    private Integer order;

    @NotNull(message = "카드 URL은 필수입니다")
    @Schema(description = "카드 이미지 URL (빈 문자열이면 해당 카드 비활성화)", example = "https://s3.../card1.jpg", required = true)
    private String cardUrl;
}

