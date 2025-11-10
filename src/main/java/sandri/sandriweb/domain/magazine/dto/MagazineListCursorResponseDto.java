package sandri.sandriweb.domain.magazine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매거진 목록 조회 응답 DTO (커서 기반 페이징)")
public class MagazineListCursorResponseDto {
    
    @Schema(description = "매거진 목록", example = "[]")
    private List<MagazineListDto> magazines;
    
    @Schema(description = "페이지 크기 (요청한 개수)", example = "10")
    private int size;
    
    @Schema(description = "다음 페이지 조회용 커서 (마지막 매거진 ID, null이면 더 이상 없음)", example = "15")
    private Long nextCursor;
    
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;
    
    @Schema(description = "전체 매거진 개수", example = "50")
    private long totalCount;
}


