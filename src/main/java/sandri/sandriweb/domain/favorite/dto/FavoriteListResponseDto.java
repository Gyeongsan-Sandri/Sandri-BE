package sandri.sandriweb.domain.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.magazine.dto.MagazineListDto;
import sandri.sandriweb.domain.place.dto.SimplePlaceDto;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 관심 목록 응답")
public class FavoriteListResponseDto {

    @Schema(description = "관심 등록한 관광지 목록")
    private List<SimplePlaceDto> places;

    @Schema(description = "관심 등록한 루트 목록")
    private List<FavoriteRouteDto> routes;

    @Schema(description = "관심 등록한 매거진 목록")
    private List<MagazineListDto> magazines;
}

