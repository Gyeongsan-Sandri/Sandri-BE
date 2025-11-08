package sandri.sandriweb.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "최근 검색어 추가 요청 DTO")
public class AddRecentSearchRequestDto {

    @NotBlank(message = "검색 키워드는 필수입니다")
    @Schema(description = "검색 키워드", example = "디저트", required = true)
    private String keyword;

    @NotNull(message = "검색 타입은 필수입니다")
    @Schema(description = "검색 타입 (PLACE, ROUTE)", example = "PLACE", required = true)
    private String searchType; // PLACE, ROUTE
}

