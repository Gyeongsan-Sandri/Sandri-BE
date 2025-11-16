package sandri.sandriweb.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Schema(description = "리뷰 작성 요청 DTO")
public class CreateReviewRequestDto {
    
    @NotNull(message = "별점은 필수입니다")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5 이하여야 합니다")
    @Schema(description = "별점 (1-5)", example = "5", required = true)
    private Integer rating;

    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(max = 1000, message = "리뷰 내용은 1000자 이하여야 합니다")
    @Schema(description = "리뷰 내용", example = "정말 좋은 장소였습니다!", required = true, maxLength = 1000)
    private String content;

    @Schema(description = "AWS S3에 업로드된 사진/영상 정보 리스트 (선택사항)")
    private List<PhotoInfo> photos;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Schema(description = "리뷰 사진 정보")
    public static class PhotoInfo {
        @NotBlank(message = "사진 URL은 필수입니다")
        @Schema(description = "AWS S3에 업로드된 사진/영상 URL", example = "https://s3.../photo1.jpg", required = true)
        private String photoUrl;
        
        @NotNull(message = "사진 순서는 필수입니다")
        @Min(value = 0, message = "사진 순서는 0 이상이어야 합니다")
        @Schema(description = "사진 순서 (0부터 시작)", example = "0", required = true)
        private Integer order;
    }
}

