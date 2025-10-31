package sandri.sandriweb.domain.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkResponseDto {
    
    private String shareUrl;
    private String shareCode;
    private String qrCodeUrl;
    
    public static ShareLinkResponseDto of(String shareUrl, String shareCode, String qrCodeUrl) {
        return ShareLinkResponseDto.builder()
                .shareUrl(shareUrl)
                .shareCode(shareCode)
                .qrCodeUrl(qrCodeUrl)
                .build();
    }
}

