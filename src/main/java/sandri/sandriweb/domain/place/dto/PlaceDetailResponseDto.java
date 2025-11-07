package sandri.sandriweb.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 상세 정보 응답 DTO")
public class PlaceDetailResponseDto {
    
    @Schema(description = "장소 ID", example = "1")
    private Long placeId;
    
    @Schema(description = "장소 이름", example = "경주 불국사")
    private String name;
    
    @Schema(description = "대분류 (관광지/맛집/카페)", example = "관광지")
    private String groupName;
    
    @Schema(description = "세부 카테고리 이름 (자연/힐링, 역사/전통, 문화/체험, 식도락)", example = "역사/전통")
    private String categoryName;
    
    @Schema(description = "리뷰 평점", example = "4.5")
    private Double rating;
    
    @Schema(description = "한글 주소", example = "경상북도 경주시 불국로 385")
    private String address;
    
    @Schema(description = "위도", example = "35.7894")
    private Double latitude;
    
    @Schema(description = "경도", example = "129.3320")
    private Double longitude;
    
    @Schema(description = "요약", example = "신라 불교 문화의 정수를 보여주는 사찰")
    private String summary;
    
    @Schema(description = "상세 정보", example = "불국사는 신라 경덕왕 10년(751)에 김대성이 창건을 시작하여...")
    private String information;
    
    @Schema(description = "공식 사진들 (순서 포함)")
    private List<PhotoDto> officialPhotos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "장소 사진 DTO")
    public static class PhotoDto {
        
        @Schema(description = "사진 순서 (0부터 시작)", example = "0")
        private Integer order;
        
        @Schema(description = "사진 URL", example = "https://s3.../photo1.jpg")
        private String photoUrl;
    }
}

