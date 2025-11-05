package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "중복 확인 응답 DTO")
public class CheckDuplicateResponseDto {
    
    @Schema(description = "중복 여부 (true: 중복됨, false: 사용 가능)", example = "false")
    private boolean isDuplicate;
    
    @Schema(description = "확인 메시지", example = "사용 가능한 아이디입니다")
    private String message;
}

