package sandri.sandriweb.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.search.entity.RecentSearch;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "최근 검색어 응답 DTO")
public class RecentSearchDto {

    @Schema(description = "최근 검색어 ID", example = "1")
    private Long id;

    @Schema(description = "검색 키워드", example = "디저트")
    private String keyword;

    @Schema(description = "검색 타입 (PLACE, ROUTE)", example = "PLACE")
    private String searchType;

    @Schema(description = "검색 일시", example = "2025-06-15T10:30:00")
    private LocalDateTime createdAt;

    public static RecentSearchDto from(RecentSearch recentSearch) {
        return RecentSearchDto.builder()
                .id(recentSearch.getId())
                .keyword(recentSearch.getKeyword())
                .searchType(recentSearch.getSearchType().name())
                .createdAt(recentSearch.getCreatedAt())
                .build();
    }
}

