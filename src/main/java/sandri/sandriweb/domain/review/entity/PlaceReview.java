package sandri.sandriweb.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.global.entity.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@SuperBuilder
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

    @Column(name = "content", length = 1000, nullable = false)
    private String content;

    @Column(name = "visit_date")
    private LocalDate visitDate; // 방문 날짜

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @OneToMany(mappedBy = "placeReview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaceReviewPhoto> photos = new ArrayList<>();

    /**
     * 리뷰 내용 수정
     * @param rating 별점
     * @param content 리뷰 내용
     */
    public void update(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
    }
}
