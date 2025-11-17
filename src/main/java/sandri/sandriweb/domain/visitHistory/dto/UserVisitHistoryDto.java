package sandri.sandriweb.domain.visitHistory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVisitHistoryDto {

    /**
     * 방문 기록 ID
     */
    private Long visitHistoryId;

    /**
     * 장소 ID
     */
    private Long placeId;

    /**
     * 장소 이름
     */
    private String placeName;

    /**
     * 장소의 첫 번째 사진 URL
     */
    private String firstPhotoUrl;

    /**
     * 방문 기록 생성 날짜 (시간 제외)
     */
    private LocalDate visitedAt;

    /**
     * 방문 요일 (한글)
     */
    private String dayOfWeek;

    /**
     * 리뷰 작성 여부
     */
    private Boolean hasReview;

    /**
     * 요일을 한글로 변환하는 헬퍼 메서드 (짧은 형식)
     */
    public static String getDayOfWeekInKorean(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}
