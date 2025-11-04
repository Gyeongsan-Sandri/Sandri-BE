package sandri.sandriweb.domain.magazine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMagazineRequestDto {

    @NotBlank(message = "매거진 이름은 필수입니다")
    private String name;

    private String summary;

    private String content;

    private List<String> cardUrls; // 매거진 카드 이미지 URL 리스트 (수정 시 기존 카드는 모두 삭제되고 새로 추가됨)
}

