package sandri.sandriweb.domain.user.dto;

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
public class SaveVisitedPlaceRequestDto {

    @NotNull(message = "장소 ID는 필수입니다")
    private Long placeId;

    @NotNull(message = "방문 날짜는 필수입니다")
    private LocalDate visitDate;
}

