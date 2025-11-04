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
public class CreateMagazineRequestDto {

    @NotBlank(message = "매거진 이름은 필수입니다")
    private String name;

    private String summary;

    private String content;

    private List<String> cardUrls; // 매거진 카드 이미지 URL 리스트 (순서대로 추가됨)
}

