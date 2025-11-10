package sandri.sandriweb.domain.place.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.admin.dto.CreatePlacePhotoRequestDto;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.enums.PlaceCategory;

import java.util.List;

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

    @Valid
    private List<CreatePlacePhotoRequestDto.PhotoInfo> photos; // 사진 정보 리스트 (photoUrl과 order 포함)
}

