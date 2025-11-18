package sandri.sandriweb.domain.place.enums;

/**
 * 장소 데이터의 출처 (우선순위 포함)
 */
public enum DataSource {
    GBGS(3),    // 경산시 관광 API (최우선)
    GOOGLE(2),  // Google Place API (두 번째)
    CSV(1);     // CSV 파일 (마지막)

    private final int priority;

    DataSource(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 우선순위 비교
     * @param other 비교 대상
     * @return 현재 소스의 우선순위가 더 높거나 같으면 true
     */
    public boolean hasHigherOrEqualPriorityThan(DataSource other) {
        if (other == null) return true;
        return this.priority >= other.priority;
    }
}
