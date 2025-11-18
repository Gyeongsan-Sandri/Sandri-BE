package sandri.sandriweb.domain.route.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "루트 장소 추가 요청 DTO")
public class AddLocationRequestDto {
    
    @NotNull(message = "장소 ID는 필수입니다")
    @Schema(description = "장소 ID", example = "1", required = true)
    private Long placeId;
    
    @NotNull(message = "일차 번호는 필수입니다")
    @Schema(description = "일차 번호", example = "1", required = true)
    private Integer dayNumber;
    
    @Schema(description = "표시 순서 (지정하지 않으면 자동으로 마지막 순서로 설정)", example = "0")
    private Integer displayOrder;
    
    @Schema(description = "장소 메모", example = "반드시 사진 찍기")
    private String memo;
}

