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
    private String address; // 한글 주소
    private String groupName; // 대분류 (관광지/맛집/카페)
    private String categoryName; // 세부 카테고리 이름 (자연/힐링, 역사/전통, 문화/체험, 식도락)
    private Double rating; // 리뷰 평점
    private Double latitude;
    private Double longitude;
    private String phone;
    private String webpage;
    private String summary;
    private String information;
    private List<String> officialPhotos; // 공식 사진들
    private List<String> reviewPhotos; // 리뷰 사진들 (요청 개수만큼)
    private List<ReviewDto> reviews; // 리뷰들 (요청 개수만큼)
    private List<NearbyPlaceDto> nearbyPlaces; // 근처 가볼만한 곳
}

