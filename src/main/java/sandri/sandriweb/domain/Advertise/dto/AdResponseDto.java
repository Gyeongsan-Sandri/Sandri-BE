package sandri.sandriweb.domain.advertise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponseDto {
    private Integer totalCount; // 전체 광고 배너 개수
    private List<AdDto> ads; // 광고 리스트
}

