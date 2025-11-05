package sandri.sandriweb.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카테고리별 장소 응답 DTO")
public class CategoryPlaceDto {
    
    @Schema(description = "장소 ID", example = "1")
    private Long placeId;
    
    @Schema(description = "장소 이름", example = "경주 불국사")
    private String name;
    
    @Schema(description = "주소", example = "경상북도 경주시 불국로 385")
    private String address;
    
    @Schema(description = "대표 사진 한 장", example = "https://s3.../photo.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "평점", example = "4.5")
    private Double rating;
    
    @Schema(description = "좋아요 개수", example = "120")
    private Integer likeCount;
    
    @Schema(description = "사용자가 좋아요한 장소인지 여부 (로그인한 경우에만 설정)", example = "true")
    private Boolean isLiked;
    
    @Schema(description = "대분류 (관광지/맛집/카페)", example = "관광지")
    private String groupName;
    
    @Schema(description = "세부 카테고리 이름", example = "역사/전통")
    private String categoryName;
}

