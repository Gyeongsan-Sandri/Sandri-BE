package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.user.entity.User;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 스타일-장소 매핑 요청 DTO")
public class MapTravelStylePlaceRequestDto {
    
    @NotNull(message = "여행 스타일은 필수입니다")
    @Schema(
            description = "여행 스타일",
            example = "ADVENTURER",
            allowableValues = {
                    "ADVENTURER", "SENSITIVE_FAIRY", "HOTSPOT_HUNTER", "LOCAL",
                    "THOROUGH_PLANNER", "HEALING_TURTLE", "WALKER", "GALLERY_PEOPLE"
            }
    )
    private User.TravelStyle travelStyle;
    
    @NotEmpty(message = "장소 ID 목록은 필수이며 최소 1개 이상이어야 합니다")
    @Schema(description = "장소 ID 목록", example = "[1, 2, 3]")
    private List<@NotNull(message = "장소 ID는 null일 수 없습니다") Long> placeIds;
}

