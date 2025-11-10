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
@Schema(description = "태그 DTO")
public class TagDto {
    
    @Schema(description = "태그 ID", example = "1")
    private Long tagId;
    
    @Schema(description = "태그 이름", example = "사진명소")
    private String name;
}

