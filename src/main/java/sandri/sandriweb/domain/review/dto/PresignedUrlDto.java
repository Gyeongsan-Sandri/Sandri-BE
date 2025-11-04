package sandri.sandriweb.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlDto {
    private String fileName;        // 파일명 (S3 키)
    private String presignedUrl;    // 업로드용 Presigned URL
    private String finalUrl;        // 업로드 완료 후 최종 접근 URL
}

