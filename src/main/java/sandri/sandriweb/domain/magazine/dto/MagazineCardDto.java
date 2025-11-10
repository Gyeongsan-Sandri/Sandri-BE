package sandri.sandriweb.domain.magazine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매거진 카드 DTO")
public class MagazineCardDto {

    @Schema(description = "카드 순서 (0부터 시작)", example = "0")
    private Integer order;

    @Schema(description = "카드뉴스 이미지 URL", example = "https://s3.../card1.jpg")
    private String cardUrl;
}

