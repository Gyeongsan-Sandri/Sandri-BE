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
    
    @Schema(description = "장소 ID", example = "1")
    private Long placeId;
    
    @Schema(description = "장소 이름", example = "경주 석굴암")
    private String name;
    
    @Schema(description = "대표 사진 한 장", example = "https://s3.../photo.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "위도", example = "35.7894")
    private Double latitude;
    
    @Schema(description = "경도", example = "129.3320")
    private Double longitude;
    
    @Schema(description = "현재 장소와 추천 위치의 거리 (미터 단위, 출력 시 원하는 단위로 변환바람)", example = "22500(2.25km일 경우 22500으로 출력)")
    private Long distanceInMeters;
    
    @Schema(description = "현재 장소로부터 가까운 거리 순위 (현재 장소는 0)", example = "1")
    private Integer rank;
}

