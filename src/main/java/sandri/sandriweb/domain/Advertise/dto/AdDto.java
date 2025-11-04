package sandri.sandriweb.domain.advertise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdDto {
    private Long adId;
    private String imageUrl; // AWS S3 이미지 URL
    private String title; // 광고 문구 (제목)
    private String description; // 광고 설명 (선택사항)
    private String linkUrl; // 클릭 시 이동할 URL (선택사항)
}

