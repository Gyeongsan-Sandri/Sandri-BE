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
@Schema(description = "HOT 관광지 응답 DTO")
public class HotPlaceDto {

    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "장소 ID", example = "10")
    private Long placeId;

    @Schema(description = "장소 이름", example = "팔공산 자락 계곡")
    private String name;

    @Schema(description = "주소", example = "경상북도 경산시 ...")
    private String address;

    @Schema(description = "썸네일 URL", example = "https://s3.../photo.jpg")
    private String thumbnailUrl;

    @Schema(description = "카테고리", example = "자연/힐링")
    private String categoryName;

    @Schema(description = "누적 좋아요 수", example = "120")
    private Long totalLikes;

    @Schema(description = "최근 7일 좋아요 수", example = "30")
    private Long recentLikes;
}
