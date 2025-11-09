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
@Schema(description = "장소 간단 정보 응답 DTO" +
                      "카테고리 목록 반환, 추천 장소 목록 반환 등" +
                      "공통 정보를 담은 DTO이므로 API에 명세한 꺼내서 사용 바람")
public class SimplePlaceDto {
    
    @Schema(description = "장소 ID", example = "1")
    private Long placeId;
    
    @Schema(description = "장소 이름", example = "경주 불국사")
    private String name;
    
    @Schema(description = "주소", example = "경상북도 경주시 불국로 385")
    private String address;
    
    @Schema(description = "대표 사진 한 장", example = "https://s3.../photo.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "사용자가 좋아요한 장소인지 여부 (로그인한 경우에만 설정)", example = "true")
    private Boolean isLiked;
    
    @Schema(description = "대분류 (관광지/맛집/카페)", example = "관광지")
    private String groupName;
    
    @Schema(description = "세부 카테고리 이름", example = "역사/전통")
    private String categoryName;
    
    @Schema(description = "기준 장소와의 거리 (미터 단위, 거리 계산이 필요한 경우에만 설정)", example = "2500")
    private Long distanceInMeters;
}

