package sandri.sandriweb.domain.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagazineListDto {
    private Long magazineId;
    private String title; // 매거진 제목
    private String thumbnail; // 썸네일 (첫 번째 카드의 이미지 URL)
    private String summary; // 매거진 요약
    private Boolean isLiked; // 사용자가 좋아요한 매거진인지 여부 (로그인한 경우에만 설정)
}

