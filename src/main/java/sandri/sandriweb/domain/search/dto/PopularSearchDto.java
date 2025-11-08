package sandri.sandriweb.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인기 검색어 응답 DTO")
public class PopularSearchDto {

    @Schema(description = "순위", example = "1")
    private Integer rank;

    @Schema(description = "검색 키워드", example = "문천지")
    private String keyword;

    @Schema(description = "순위 변동 (UP, DOWN, SAME)", example = "UP")
    private String rankChange;

    @Schema(description = "이전 순위 (변동이 있는 경우)", example = "3")
    private Integer previousRank;
}

