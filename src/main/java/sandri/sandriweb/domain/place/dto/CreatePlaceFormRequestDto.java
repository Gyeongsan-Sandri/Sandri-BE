package sandri.sandriweb.domain.place.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Swagger 폼에서 장소 생성 시 파일 업로드까지 한 번에 처리하기 위한 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "장소 생성 폼 요청 DTO (이미지 업로드 포함)")
public class CreatePlaceFormRequestDto extends CreatePlaceRequestDto {

    @ArraySchema(arraySchema = @Schema(description = "업로드할 장소 사진 목록"),
            schema = @Schema(type = "string", format = "binary"))
    private List<MultipartFile> photos;
}

