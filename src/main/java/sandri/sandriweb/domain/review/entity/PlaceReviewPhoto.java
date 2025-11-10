package sandri.sandriweb.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.global.entity.BaseEntity;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "places_review_photos")
public class PlaceReviewPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_review_photo_id")
    private Long id;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;
    
    @Column(name = "`order`", nullable = false)
    private Integer order; // 사진 순서 (0부터 시작)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_review_id")
    private PlaceReview placeReview;

    /**
     * 사진 URL 수정
     * @param photoUrl 사진 URL
     */
    public void updatePhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
