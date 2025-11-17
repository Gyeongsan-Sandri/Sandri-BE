package sandri.sandriweb.domain.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.route.entity.Route;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관심 등록된 루트 요약 DTO")
public class FavoriteRouteDto {

    @Schema(description = "루트 ID", example = "12")
    private Long routeId;

    @Schema(description = "루트 제목", example = "경주 1박 2일 코스")
    private String title;

    @Schema(description = "여행 시작일", example = "2025-03-01")
    private LocalDate startDate;

    @Schema(description = "여행 종료일", example = "2025-03-02")
    private LocalDate endDate;

    @Schema(description = "사용자 좋아요 여부", example = "true")
    private Boolean isLiked;

    @Schema(description = "대표 이미지 URL", example = "https://s3.amazonaws.com/bucket/route-cover.jpg")
    private String imageUrl;

    public static FavoriteRouteDto from(Route route) {
        return FavoriteRouteDto.builder()
                .routeId(route.getId())
                .title(route.getTitle())
                .startDate(route.getStartDate())
                .endDate(route.getEndDate())
                .isLiked(true)
                .imageUrl(route.getImageUrl())
                .build();
    }
}

