package sandri.sandriweb.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.place.entity.Place;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyPlaceDto {
    private String name; // 장소 이름
    private String thumbnailUrl; // 대표 사진 한 장
    private Long distanceInMeters; // 현재 장소와 추천 위치의 거리 (미터 단위)
    private String categoryName; // 카테고리 이름
}

