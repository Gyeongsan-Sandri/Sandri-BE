package sandri.sandriweb.domain.visitHistory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 방문 확인 응답 DTO")
public class VisitPlaceResponseDto {

    @Schema(description = "방문 여부", example = "true")
    private boolean visited;

    @Schema(description = "방문 기록 ID (방문한 경우에만 제공)", example = "1")
    private Long visitHistoryId;
}

