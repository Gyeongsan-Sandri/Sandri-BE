package sandri.sandriweb.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorResponseDto<T> {
    private List<T> content;
    private int size;
    private Long nextCursor; // 다음 페이지를 가져오기 위한 마지막 리뷰 ID (null이면 더 이상 없음)
    private boolean hasNext; // 다음 페이지 존재 여부
    private Long totalCount; // 전체 개수 (리뷰 목록 조회 시 총 리뷰 개수)
}

