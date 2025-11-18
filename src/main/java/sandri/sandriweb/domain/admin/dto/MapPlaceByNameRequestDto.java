package sandri.sandriweb.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 이름으로 매핑 요청 DTO")
public class MapPlaceByNameRequestDto {

    @NotBlank(message = "장소 이름은 필수입니다")
    @Schema(description = "장소 이름", example = "경주 불국사", required = true)
    private String placeName;
}

