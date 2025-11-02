package sandri.sandriweb.domain.magazine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMagazineCardRequestDto {

    @NotNull(message = "매거진 ID는 필수입니다")
    private Long magazineId;

    @NotBlank(message = "카드 이미지 URL은 필수입니다")
    private String cardUrl;
}

