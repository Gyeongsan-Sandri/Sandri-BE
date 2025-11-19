package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 스타일별 장소 정보 응답 DTO")
public class TravelStylePlaceResponseDto {
    
    @Schema(description = "장소 ID", example = "1")
    private Long placeId;
    
    @Schema(description = "장소 이름", example = "경주 불국사")
    private String name;
    
    @Schema(description = "썸네일 사진 URL", example = "https://s3.../photo.jpg")
    private String thumbnailUrl;
}

