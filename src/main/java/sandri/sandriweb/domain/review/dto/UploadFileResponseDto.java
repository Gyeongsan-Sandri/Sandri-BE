package sandri.sandriweb.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileResponseDto {
    private List<String> fileUrls; // 업로드된 파일들의 URL 리스트
}

