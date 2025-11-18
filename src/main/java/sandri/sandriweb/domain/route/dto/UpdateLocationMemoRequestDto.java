package sandri.sandriweb.domain.route.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "루트 장소 메모 저장 요청 DTO")
public class UpdateLocationMemoRequestDto {

    @Size(max = 1000, message = "메모는 1000자 이하여야 합니다.")
    @Schema(description = "장소 메모 내용 (null 또는 빈 문자열 입력 시 삭제)", example = "도착하면 사진 먼저 찍기")
    private String memo;
}


