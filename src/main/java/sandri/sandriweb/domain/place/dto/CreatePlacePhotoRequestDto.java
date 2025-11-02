package sandri.sandriweb.domain.place.dto;

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
public class CreatePlacePhotoRequestDto {

    @NotNull(message = "장소 ID는 필수입니다")
    private Long placeId;

    @NotBlank(message = "사진 URL은 필수입니다")
    private String photoUrl;
}

