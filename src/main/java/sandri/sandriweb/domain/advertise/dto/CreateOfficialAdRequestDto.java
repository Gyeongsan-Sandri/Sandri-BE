package sandri.sandriweb.domain.advertise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Schema(description = "공식 광고 생성 요청 DTO")
public class CreateOfficialAdRequestDto {

    @NotBlank(message = "광고 제목은 필수입니다")
    @Schema(description = "광고 제목", example = "봄 맞이 경주 벚꽃 축제", required = true)
    private String title;

    @Schema(description = "광고 설명", example = "경주 전역에서 즐기는 벚꽃 명소 추천")
    private String description;

    @NotBlank(message = "이미지 URL은 필수입니다")
    @Schema(description = "광고 이미지 URL", example = "https://s3.../ad1.png", required = true)
    private String imageUrl;

    @Schema(description = "랜딩 링크", example = "https://sandri.kr/events/1")
    private String linkUrl;

    @Schema(description = "노출 시작일시 (UTC)", example = "2025-03-15T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "노출 종료일시 (UTC)", example = "2025-03-31T23:59:00")
    private LocalDateTime endDate;

    @Builder.Default
    @Schema(description = "노출 순서 (낮을수록 먼저 노출)", example = "0")
    private Integer displayOrder = 0;
}

