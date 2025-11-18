package sandri.sandriweb.domain.route.enums;

import org.springframework.util.StringUtils;

/**
 * 루트 목록 정렬 타입
 */
public enum RouteSortType {
    PINNED,
    LATEST,
    OLDEST;

    public static RouteSortType from(String value) {
        if (!StringUtils.hasText(value)) {
            return LATEST;
        }

        try {
            return RouteSortType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원하지 않는 정렬 방식입니다: " + value);
        }
    }
}


