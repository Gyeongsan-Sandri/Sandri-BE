package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방문 장소 저장 요청 DTO")
public class SaveVisitedPlaceRequestDto {

    @NotNull(message = "장소 ID는 필수입니다")
    @Schema(description = "장소 ID", example = "1", required = true)
    private Long placeId;

    @NotNull(message = "방문 날짜는 필수입니다")
    @Schema(description = "방문 날짜", example = "2024-11-05", required = true)
    private LocalDate visitDate;
}

