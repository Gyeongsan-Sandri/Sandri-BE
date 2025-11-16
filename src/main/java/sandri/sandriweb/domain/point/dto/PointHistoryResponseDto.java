package sandri.sandriweb.domain.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryResponseDto {

    /**
     * 7일 이내 소멸 예정 포인트
     */
    private Long expiringPointsWithin7Days;

    /**
     * 포인트 히스토리 목록
     */
    private List<PointHistoryDto> histories;
}
