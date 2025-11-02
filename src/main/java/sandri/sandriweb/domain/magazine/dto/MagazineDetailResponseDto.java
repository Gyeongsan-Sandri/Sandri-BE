package sandri.sandriweb.domain.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagazineDetailResponseDto {
    private Long magazineId;
    private String name; // 매거진 이름
    private String summary; // 매거진 요약
    private String content; // 매거진 내용
    private List<MagazineCardDto> cards; // 매거진 카드 리스트 (순서대로)
}

