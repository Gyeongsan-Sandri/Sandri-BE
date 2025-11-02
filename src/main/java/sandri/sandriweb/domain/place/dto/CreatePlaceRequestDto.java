package sandri.sandriweb.domain.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.enums.PlaceCategory;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlaceRequestDto {

    @NotBlank(message = "장소 이름은 필수입니다")
    private String name;

    private String address;

    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    private String phone;

    private String webpage;

    private String summary;

    private String information;

    @NotNull(message = "대분류는 필수입니다")
    private PlaceCategory group; // 관광지/맛집/카페

    @NotNull(message = "세부 카테고리는 필수입니다")
    private Category category; // 자연/힐링, 역사/전통, 문화/체험, 식도락
}

