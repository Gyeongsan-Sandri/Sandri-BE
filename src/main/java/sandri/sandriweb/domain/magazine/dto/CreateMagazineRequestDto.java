package sandri.sandriweb.domain.magazine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Schema(description = "매거진 생성 요청 DTO")
public class CreateMagazineRequestDto {

    @NotBlank(message = "매거진 이름은 필수입니다")
    @Schema(description = "매거진 이름", example = "경주 여행 완벽 가이드", required = true)
    private String name;

    @Schema(description = "매거진 요약", example = "경주의 대표 관광지를 한눈에 볼 수 있는 가이드")
    private String summary;

    @Schema(description = "매거진 내용", example = "경주는 신라 천년의 고도로...")
    private String content;

    @Schema(
            description = "카드 정보 리스트 (order와 cardUrl). Swagger 폼에서는 cards[0].order 형태로 입력합니다.",
            example = "[{\"order\":0,\"cardUrl\":\"https://s3.../card1.jpg\"}]"
    )
    private List<MagazineCardInfoDto> cards;
}

