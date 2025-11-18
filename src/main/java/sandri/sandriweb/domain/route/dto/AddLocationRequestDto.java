package sandri.sandriweb.domain.route.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "루트 장소 추가 요청 DTO")
public class AddLocationRequestDto {
    
    @Schema(description = "장소 ID (DB에 있는 장소일 경우)", example = "1")
    private Long placeId;
    
    @Schema(description = "Google Places 정보 (placeId가 null일 경우 필수)", example = "{\"name\": \"경주 불국사\", \"address\": \"경상북도 경주시 불국로 385\", \"latitude\": 35.7894, \"longitude\": 129.3320, \"types\": [\"tourist_attraction\", \"point_of_interest\"]}")
    private GooglePlaceInfo googlePlaceInfo;
    
    @NotNull(message = "일차 번호는 필수입니다")
    @Schema(description = "일차 번호", example = "1", required = true)
    private Integer dayNumber;
    
    @Schema(description = "표시 순서 (지정하지 않으면 자동으로 마지막 순서로 설정)", example = "0")
    private Integer displayOrder;
    
    @Schema(description = "장소 메모", example = "반드시 사진 찍기")
    private String memo;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Google Places 정보")
    public static class GooglePlaceInfo {
        @Schema(description = "장소 이름", example = "경주 불국사", required = true)
        private String name;
        
        @Schema(description = "주소", example = "경상북도 경주시 불국로 385")
        private String address;
        
        @Schema(description = "위도", example = "35.7894", required = true)
        private Double latitude;
        
        @Schema(description = "경도", example = "129.3320", required = true)
        private Double longitude;
        
        @Schema(description = "Google Places types", example = "[\"tourist_attraction\", \"point_of_interest\"]")
        private List<String> types;
        
        @Schema(description = "사진 URL", example = "https://maps.googleapis.com/maps/api/place/photo?...")
        private String photoUrl;
    }
}

