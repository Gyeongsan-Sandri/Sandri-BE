package sandri.sandriweb.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매거진 카드에 장소 매핑 요청 DTO")
public class MapPlaceToCardRequestDto {

    @Schema(description = "장소 ID (null이면 매핑 해제)", example = "1", required = false)
    private Long placeId;
}

