package sandri.sandriweb.domain.magazine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매거진 카드뉴스에 매핑된 장소 썸네일 정보 DTO")
public class MagazinePlaceThumbnailDto {
    
    @Schema(description = "장소 ID", example = "1")
    private Long placeId;
    
    @Schema(description = "장소 이름", example = "경주 불국사")
    private String name;
    
    @Schema(description = "썸네일 (대표 사진 한 장)", example = "https://s3.../photo.jpg")
    private String thumbnailUrl;
}

