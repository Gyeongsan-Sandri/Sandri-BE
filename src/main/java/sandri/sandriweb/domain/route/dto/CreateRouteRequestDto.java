package sandri.sandriweb.domain.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateRouteRequestDto {
    
    @NotBlank(message = "루트 제목은 필수입니다")
    private String title;
    
    private String description;
    
    @NotNull(message = "시작 날짜는 필수입니다")
    private LocalDate startDate;
    
    @NotNull(message = "종료 날짜는 필수입니다")
    private LocalDate endDate;
    
    @Builder.Default
    private boolean isPublic = false;
    
    private List<LocationDto> locations;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private Integer dayNumber;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String description;
        private Integer displayOrder;
    }
}

