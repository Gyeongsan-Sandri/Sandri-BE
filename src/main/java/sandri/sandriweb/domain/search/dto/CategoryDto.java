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
@Schema(description = "카테고리 응답 DTO")
public class CategoryDto {

    @Schema(description = "카테고리 코드", example = "자연_힐링")
    private String code;

    @Schema(description = "카테고리 이름", example = "자연/힐링")
    private String name;
}

