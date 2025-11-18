package sandri.sandriweb.domain.visitHistory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 방문 확인 요청 DTO (위치 정보)")
public class VisitPlaceRequestDto {

    @NotNull(message = "위도는 필수입니다")
    @Schema(description = "현재 사용자 GPS 위도", example = "35.8251", required = true)
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    @Schema(description = "현재 사용자 GPS 경도", example = "128.7405", required = true)
    private Double longitude;
}

