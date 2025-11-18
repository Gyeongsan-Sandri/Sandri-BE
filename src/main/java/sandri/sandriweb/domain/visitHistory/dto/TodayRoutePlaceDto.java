package sandri.sandriweb.domain.visitHistory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayRoutePlaceDto {
    /**
     * 장소 정보
     */
    private PlaceInfo placeInfo;
    
    /**
     * 여행의 총 장소 개수
     */
    private Integer totalPlaceCount;
    
    /**
     * 해당 여행지의 방문 순서 (displayOrder)
     */
    private Integer visitOrder;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceInfo {
        /**
         * 썸네일 URL (첫 번째 사진)
         */
        private String thumbnail;
        
        /**
         * 장소 이름
         */
        private String placeName;
        
        /**
         * 한글 주소
         */
        private String address;
    }
}

