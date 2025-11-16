package sandri.sandriweb.domain.point.enums;

/**
 * 포인트 히스토리 조회 타입
 */
public enum PointHistoryType {
    ALL,    // 전체 조회
    EARN,   // 적립 포인트만 조회 (양수)
    USE     // 사용 포인트만 조회 (음수)
}
