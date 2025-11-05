package sandri.sandriweb.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.review.entity.PlaceReview;
import sandri.sandriweb.domain.user.dto.UserProfileDto;

import java.time.LocalDateTime;
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
    
    @Schema(description = "사진 URL 리스트", example = "[\"https://s3.../photo1.jpg\"]")
    private List<String> photoUrls;

    public static ReviewDto from(PlaceReview review) {
        List<String> photoUrls = review.getPhotos() != null 
                ? review.getPhotos().stream()
                        .map(photo -> photo.getPhotoUrl())
                        .collect(Collectors.toList())
                : List.of();

        return ReviewDto.builder()
                .reviewId(review.getId())
                .user(UserProfileDto.from(review.getUser()))
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .photoUrls(photoUrls)
                .build();
    }
}

