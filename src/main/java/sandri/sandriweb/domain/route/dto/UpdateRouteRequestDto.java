package sandri.sandriweb.domain.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRouteRequestDto {
    
    private String title;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Boolean isPublic;

    private String imageUrl;
    
    private List<LocationDto> locations;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private Long id;
        private Integer dayNumber;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String description;
        private Integer displayOrder;
    }
}

