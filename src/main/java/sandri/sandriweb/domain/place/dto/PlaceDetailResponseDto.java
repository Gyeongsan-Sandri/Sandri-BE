package sandri.sandriweb.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetailResponseDto {
    private Long placeId;
    private String name;
    private String groupName; // 대분류 (관광지/맛집/카페)
    private String categoryName; // 세부 카테고리 이름 (자연/힐링, 역사/전통, 문화/체험, 식도락)
    private Double rating; // 리뷰 평점
    private String address; // 한글 주소
    private Double latitude;
    private Double longitude;
    private String summary;
    private String information;
    private List<String> officialPhotos; // 공식 사진들
}

