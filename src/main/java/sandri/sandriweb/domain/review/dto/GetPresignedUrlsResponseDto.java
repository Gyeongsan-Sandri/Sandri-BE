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
public class GetPresignedUrlsResponseDto {
    private List<PresignedUrlDto> presignedUrls; // Presigned URL 리스트
}

