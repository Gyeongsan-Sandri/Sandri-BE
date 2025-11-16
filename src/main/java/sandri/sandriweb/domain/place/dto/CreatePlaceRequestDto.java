package sandri.sandriweb.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.enums.PlaceCategory;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Schema(description = "관리자용 장소 생성 요청 DTO")
public class CreatePlaceRequestDto {

    @NotBlank(message = "장소 이름은 필수입니다")
    @Schema(description = "장소 이름", example = "경주 불국사", required = true)
    private String name;

    @Schema(description = "주소", example = "경상북도 경주시 불국로 385")
    private String address;

    @Schema(description = "위도 (주소만 입력하면 자동 계산됩니다)", example = "35.7894")
    private Double latitude;

    @Schema(description = "경도 (주소만 입력하면 자동 계산됩니다)", example = "129.332")
    private Double longitude;

    @Schema(description = "한 줄 요약", example = "신라 불교 문화의 정수를 보여주는 사찰")
    private String summary;

    @Schema(description = "상세 설명", example = "불국사는 1995년 유네스코 세계문화유산으로 등재되었습니다.")
    private String information;

    @NotNull(message = "대분류는 필수입니다")
    @Schema(description = "대분류 (관광지/맛집/카페)", example = "관광지", required = true)
    private PlaceCategory group; // 관광지/맛집/카페

    @NotNull(message = "세부 카테고리는 필수입니다")
    @Schema(description = "세부 카테고리 (자연_힐링/역사_전통/문화_체험/식도락)", example = "역사_전통", required = true)
    private Category category; // 자연/힐링, 역사/전통, 문화/체험, 식도락
}

