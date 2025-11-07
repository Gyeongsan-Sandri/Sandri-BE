package sandri.sandriweb.domain.place.dto;

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
public class UpdatePlaceRequestDto {

    private String name;

    private String address;

    private Double latitude;

    private Double longitude;

    private String summary;

    private String information;

    private PlaceCategory group; // 관광지/맛집/카페

    private Category category; // 자연/힐링, 역사/전통, 문화/체험, 식도락
}

