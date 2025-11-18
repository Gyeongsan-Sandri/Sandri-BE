package sandri.sandriweb.domain.route.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "HOT 루트 응답 DTO")
public class HotRouteDto {

    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "루트 ID", example = "10")
    private Long routeId;

    @Schema(description = "루트 제목", example = "경주 2박 3일 여행")
    private String title;

    @Schema(description = "시작 날짜", example = "2025-01-15")
    private LocalDate startDate;

    @Schema(description = "종료 날짜", example = "2025-01-17")
    private LocalDate endDate;

    @Schema(description = "루트 이미지 URL", example = "https://s3.../route.jpg")
    private String imageUrl;

    @Schema(description = "작성자 ID", example = "5")
    private Long creatorId;

    @Schema(description = "작성자 닉네임", example = "여행러버")
    private String creatorNickname;

    @Schema(description = "누적 좋아요 수", example = "150")
    private Long totalLikes;

    @Schema(description = "최근 7일 좋아요 수", example = "45")
    private Long recentLikes;
}

