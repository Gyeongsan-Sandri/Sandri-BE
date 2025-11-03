package sandri.sandriweb.domain.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagazineCardDto {
    private Integer order; // 카드 순서 (0부터 시작)
    private String cardUrl; // 카드뉴스 이미지 URL
}

