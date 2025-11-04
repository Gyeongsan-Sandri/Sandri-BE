package sandri.sandriweb.domain.place.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlacePhotoRequestDto {

    @NotBlank(message = "사진 URL은 필수입니다")
    private String photoUrl;
}

