package sandri.sandriweb.domain.route.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "루트 생성 요청 DTO")
public class CreateRouteRequestDto {
    
    @NotBlank(message = "루트 제목은 필수입니다")
    @Schema(description = "루트 제목", example = "경주 2박 3일 여행", required = true)
    private String title;
    
    @NotNull(message = "시작 날짜는 필수입니다")
    @Schema(description = "시작 날짜", example = "2024-11-05", required = true)
    private LocalDate startDate;
    
    @NotNull(message = "종료 날짜는 필수입니다")
    @Schema(description = "종료 날짜", example = "2024-11-07", required = true)
    private LocalDate endDate;
    
    @Builder.Default
    @Schema(description = "공개 여부", example = "false")
    private boolean isPublic = false;
    
    @Schema(description = "대표 이미지 URL", example = "https://s3.amazonaws.com/bucket/route-cover.jpg")
    private String imageUrl;
    
    @Schema(description = "장소 목록")
    private List<LocationDto> locations;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "루트 장소 정보")
    public static class LocationDto {
        
        @Schema(description = "일차 번호", example = "1")
        private Integer dayNumber;
        
        @Schema(description = "장소 이름", example = "경주 불국사")
        private String name;
        
        @Schema(description = "장소 주소", example = "경상북도 경주시 불국로 385")
        private String address;
        
        @Schema(description = "위도", example = "35.7894")
        private Double latitude;
        
        @Schema(description = "경도", example = "129.3320")
        private Double longitude;
        
        @Schema(description = "설명", example = "신라 불교 문화의 정수")
        private String description;
        
        @Schema(description = "표시 순서", example = "0")
        private Integer displayOrder;
    }
}

