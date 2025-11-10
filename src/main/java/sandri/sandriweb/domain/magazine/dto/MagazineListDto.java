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
@Schema(description = "매거진 목록 DTO")
public class MagazineListDto {
    
    @Schema(description = "매거진 ID", example = "1")
    private Long magazineId;
    
    @Schema(description = "매거진 제목", example = "경주 여행 완벽 가이드")
    private String title;
    
    @Schema(description = "썸네일 (첫 번째 카드의 이미지 URL)", example = "https://s3.../card1.jpg")
    private String thumbnail;
    
    @Schema(description = "매거진 요약", example = "경주의 대표 관광지를 한눈에 볼 수 있는 가이드")
    private String summary;
    
    @Schema(description = "사용자가 좋아요한 매거진인지 여부 (로그인한 경우에만 설정, 비로그인 시 null)", example = "true")
    private Boolean isLiked;
    
    @Schema(description = "태그 리스트", example = "[]")
    private List<TagDto> tags;
}

