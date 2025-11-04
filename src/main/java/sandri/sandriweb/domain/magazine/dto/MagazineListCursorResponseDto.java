package sandri.sandriweb.domain.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagazineListCursorResponseDto {
    private List<MagazineListDto> magazines;
    private int size;
    private Long nextCursor;
    private boolean hasNext;
    private long totalCount; // 전체 매거진 개수
}


