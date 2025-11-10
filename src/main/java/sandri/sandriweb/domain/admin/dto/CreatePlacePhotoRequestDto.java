package sandri.sandriweb.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 사진 추가 요청 DTO")
public class CreatePlacePhotoRequestDto {

    @NotNull(message = "사진 정보 리스트는 필수입니다")
    @Size(min = 1, message = "최소 1개 이상의 사진이 필요합니다")
    @Valid
    @Schema(description = "사진 정보 리스트 (photoUrl과 order 포함)")
    private List<PhotoInfo> photos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사진 정보")
    public static class PhotoInfo {
        @NotBlank(message = "사진 URL은 필수입니다")
        @Schema(description = "사진 URL", example = "https://s3.../photo1.jpg", required = true)
        private String photoUrl;

        @NotNull(message = "사진 순서는 필수입니다")
        @Min(value = 0, message = "사진 순서는 0 이상이어야 합니다")
        @Schema(description = "사진 순서 (0부터 시작)", example = "0", required = true)
        private Integer order;
    }
}

