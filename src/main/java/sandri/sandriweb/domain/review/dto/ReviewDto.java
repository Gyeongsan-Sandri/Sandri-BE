package sandri.sandriweb.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.review.entity.PlaceReview;
import sandri.sandriweb.domain.review.entity.PlaceReviewPhoto;
import sandri.sandriweb.domain.user.dto.UserProfileDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "리뷰 응답 DTO")
public class ReviewDto {
    
    @Schema(description = "리뷰 ID", example = "1")
    private Long reviewId;
    
    @Schema(description = "작성자 정보")
    private UserProfileDto user;
    
    @Schema(description = "리뷰 내용", example = "정말 좋은 장소였습니다!")
    private String content;
    
    @Schema(description = "별점 (1-5)", example = "5")
    private Integer rating;
    
    @Schema(description = "작성 일시", example = "2024-11-05T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "사진 정보 리스트")
    private List<PhotoDto> photos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리뷰 사진 정보")
    public static class PhotoDto {
        @Schema(description = "사진 URL", example = "https://s3.../photo1.jpg")
        private String photoUrl;
        
        @Schema(description = "사진 순서 (0부터 시작)", example = "0")
        private Integer order;
    }

    public static ReviewDto from(PlaceReview review) {
        return from(review, true);
    }

    public static ReviewDto from(PlaceReview review, boolean includeUser) {
        // enabled된 사진만 필터링하고 order 순서로 정렬한 후, 0부터 연속적으로 재정렬
        List<PlaceReviewPhoto> enabledPhotos = review.getPhotos() != null 
                ? review.getPhotos().stream()
                        .filter(photo -> photo.isEnabled()) // enabled된 사진만
                        .sorted((p1, p2) -> Integer.compare(p1.getOrder(), p2.getOrder())) // order 순서로 정렬
                        .collect(Collectors.toList())
                : List.of();
        
        // order를 0부터 연속적으로 재정렬
        List<PhotoDto> photoDtos = new ArrayList<>();
        int index = 0;
        for (PlaceReviewPhoto photo : enabledPhotos) {
            photoDtos.add(PhotoDto.builder()
                    .photoUrl(photo.getPhotoUrl())
                    .order(index++) // 0부터 연속적으로 재정렬
                    .build());
        }

        return ReviewDto.builder()
                .reviewId(review.getId())
                .user(includeUser ? UserProfileDto.from(review.getUser()) : null)
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .photos(photoDtos)
                .build();
    }
}

