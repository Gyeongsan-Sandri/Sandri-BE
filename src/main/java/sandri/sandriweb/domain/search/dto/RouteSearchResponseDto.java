package sandri.sandriweb.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "루트 검색 결과 응답 DTO")
public class RouteSearchResponseDto {

    @Schema(description = "검색 결과 목록")
    private List<RouteSearchItemDto> routes;

    @Schema(description = "전체 검색 결과 수", example = "15")
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
    @Schema(description = "루트 검색 결과 항목")
    public static class RouteSearchItemDto {
        @Schema(description = "루트 ID", example = "1")
        private Long routeId;

        @Schema(description = "루트 제목", example = "빵순이를 위한 빵 루트")
        private String title;

        @Schema(description = "시작 날짜", example = "2025-06-01")
        private LocalDate startDate;

        @Schema(description = "종료 날짜", example = "2025-06-02")
        private LocalDate endDate;

        @Schema(description = "작성자 닉네임", example = "홍길동")
        private String creatorNickname;

        @Schema(description = "대표 이미지 URL", example = "https://s3.../photo.jpg")
        private String thumbnailUrl;

        @Schema(description = "해시태그 목록", example = "[\"#식도락\", \"#베이커리\"]")
        private List<String> hashtags;
    }
}

