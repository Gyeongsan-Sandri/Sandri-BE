package sandri.sandriweb.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.global.entity.BaseEntity;

import java.util.List;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "place_reviews")
public class PlaceReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_review_id")
    private Long id;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @OneToMany(mappedBy = "placeReview")
    private List<PlaceReviewPhoto> photos;
}
