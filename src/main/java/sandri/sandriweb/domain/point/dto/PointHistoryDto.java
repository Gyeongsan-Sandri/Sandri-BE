package sandri.sandriweb.domain.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryDto {

    /**
     * 포인트 히스토리 생성 날짜 (시간 제외)
     */
    private LocalDate createdAt;

    /**
     * 적립 조건 타입 제목 (예: "회원가입", "방문 포인트" 등)
     */
    private String conditionTypeTitle;

    /**
     * 포인트 증감 값 (적립: 양수, 사용: 음수)
     */
    private Long pointAmount;

    /**
     * 포인트 변동 후 잔액
     */
    private Long balanceAfter;
}
