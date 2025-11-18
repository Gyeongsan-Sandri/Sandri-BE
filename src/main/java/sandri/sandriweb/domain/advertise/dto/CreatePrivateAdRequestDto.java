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
@Schema(description = "개인 광고 생성 요청 DTO")
public class CreatePrivateAdRequestDto {

    @NotBlank(message = "광고 제목은 필수입니다")
    @Schema(description = "광고 제목", example = "게스트하우스 신규 오픈 프로모션", required = true)
    private String title;

    @Schema(description = "광고 설명", example = "오픈 기념 20% 할인 쿠폰 제공")
    private String description;

    @NotBlank(message = "이미지 URL은 필수입니다")
    @Schema(description = "광고 이미지 URL", example = "https://s3.../private-ad.png", required = true)
    private String imageUrl;

    @Schema(description = "연결 링크", example = "https://partner-site.kr/event")
    private String linkUrl;

    @Schema(description = "노출 시작일시 (UTC)", example = "2025-04-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "노출 종료일시 (UTC)", example = "2025-04-15T23:59:00")
    private LocalDateTime endDate;

    @Builder.Default
    @Schema(description = "노출 순서 (낮을수록 먼저)", example = "0")
    private Integer displayOrder = 0;
}

