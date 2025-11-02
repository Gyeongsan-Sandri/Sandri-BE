package sandri.sandriweb.domain.advertise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrivateAdRequestDto {

    @NotBlank(message = "광고 제목은 필수입니다")
    private String title;

    private String description;

    @NotBlank(message = "이미지 URL은 필수입니다")
    private String imageUrl;

    private String linkUrl;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Builder.Default
    private Integer displayOrder = 0;
}

