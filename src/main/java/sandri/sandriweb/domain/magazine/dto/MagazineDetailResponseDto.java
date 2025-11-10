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
@Schema(description = "매거진 상세 정보 응답 DTO")
public class MagazineDetailResponseDto {
    
    @Schema(description = "매거진 ID", example = "1")
    private Long magazineId;
    
    @Schema(description = "매거진 내용", example = "경주는 신라 천년의 고도로...")
    private String content;
    
    @Schema(description = "카드뉴스 총 개수", example = "5")
    private Integer cardCount;
    
    @Schema(description = "매거진 카드 리스트 (순서대로)")
    private List<MagazineCardDto> cards;
}

