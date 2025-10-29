package sandri.sandriweb.domain.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResponseDto {
    
    private String status;
    private String message;
    private List<PlaceDto> results;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDto {
        private String placeId;
        private String name;
        private String formattedAddress;
        private LocationDto location;
        private Double rating;
        
        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class LocationDto {
            private Double lat;
            private Double lng;
        }
    }
}

