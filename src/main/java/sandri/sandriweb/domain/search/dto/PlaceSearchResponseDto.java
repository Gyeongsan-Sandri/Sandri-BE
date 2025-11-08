package sandri.sandriweb.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 검색 결과 응답 DTO")
public class PlaceSearchResponseDto {

    @Schema(description = "검색 결과 목록")
    private List<PlaceSearchItemDto> places;

    @Schema(description = "전체 검색 결과 수", example = "25")
    private Long totalCount;

    @Schema(description = "현재 페이지", example = "1")
    private Integer page;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private Boolean hasNext;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "장소 검색 결과 항목")
    public static class PlaceSearchItemDto {
        @Schema(description = "장소 ID", example = "1")
        private Long placeId;

        @Schema(description = "장소 이름", example = "경주 불국사")
        private String name;

        @Schema(description = "주소", example = "경상북도 경주시 불국로 385")
        private String address;

        @Schema(description = "대표 사진 URL", example = "https://s3.../photo.jpg")
        private String thumbnailUrl;

        @Schema(description = "평점", example = "4.5")
        private Double rating;

        @Schema(description = "좋아요 개수", example = "120")
        private Integer likeCount;

        @Schema(description = "대분류 (관광지/맛집/카페)", example = "관광지")
        private String groupName;

        @Schema(description = "세부 카테고리", example = "역사/전통")
        private String categoryName;

        @Schema(description = "해시태그 목록", example = "[\"#식도락\", \"#감성카페\"]")
        private List<String> hashtags;
    }
}

