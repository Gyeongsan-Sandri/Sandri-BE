package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.user.entity.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 스타일 저장 요청 DTO")
public class SaveTravelStyleRequestDto {
    
    @NotNull(message = "여행 스타일은 필수입니다")
    @Schema(description = "여행 스타일", example = "ADVENTURE", required = true, allowableValues = {"ADVENTURE", "RELAXATION", "CULTURE", "NATURE", "FOOD", "SHOPPING"})
    private User.TravelStyle travelStyle;
}

