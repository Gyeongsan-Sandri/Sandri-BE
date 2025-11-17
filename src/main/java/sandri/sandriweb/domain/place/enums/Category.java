package sandri.sandriweb.domain.place.enums;

import java.util.Arrays;

public enum Category {
    자연_힐링("자연/힐링"),
    역사_전통("역사/전통"),
    문화_체험("문화/체험"),
    식도락("식도락");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * displayName으로 Category enum 찾기
     */
    public static Category fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(c -> c.displayName.equals(displayName))
                .findFirst()
                .orElse(null);
    }
}

