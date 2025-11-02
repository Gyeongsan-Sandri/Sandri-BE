package sandri.sandriweb.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.review.entity.PlaceReview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long reviewId;
    private UserProfileDto user;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
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

