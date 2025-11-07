package sandri.sandriweb.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.place.entity.Place;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "근처 장소 응답 DTO")
public class NearbyPlaceDto {
    
    @Schema(description = "장소 이름", example = "경주 석굴암")
    private String name;
    
    @Schema(description = "대표 사진 한 장", example = "https://s3.../photo.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "현재 장소와 추천 위치의 거리 (미터 단위)", example = "2500")
    private Long distanceInMeters;
    
    @Schema(description = "카테고리 이름", example = "역사/전통")
    private String categoryName;
}

