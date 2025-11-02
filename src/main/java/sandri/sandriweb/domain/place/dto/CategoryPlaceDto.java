package sandri.sandriweb.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPlaceDto {
    private Long placeId;
    private String name; // 장소 이름
    private String address; // 주소
    private String thumbnailUrl; // 대표 사진 한 장
    private Double rating; // 평점
    private Integer likeCount; // 좋아요 개수
    private Boolean isLiked; // 사용자가 좋아요한 장소인지 여부 (로그인한 경우에만 설정)
    private String groupName; // 대분류 (관광지/맛집/카페)
    private String categoryName; // 세부 카테고리 이름
}

