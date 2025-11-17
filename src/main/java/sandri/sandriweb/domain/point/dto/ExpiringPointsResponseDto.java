package sandri.sandriweb.domain.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringPointsResponseDto {

    /**
     * 7일 이내 소멸 예정 포인트
     */
    private Long expiringPointsWithin7Days;
}
