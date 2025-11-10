package sandri.sandriweb.domain.review.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequestDto {
    
    @NotNull(message = "별점은 필수입니다")
    private Integer rating;

    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(max = 1000, message = "리뷰 내용은 1000자 이하여야 합니다")
    private String content;

    private List<CreateReviewRequestDto.PhotoInfo> photos; // AWS S3에 업로드된 사진/영상 정보 리스트 (선택사항)
}

