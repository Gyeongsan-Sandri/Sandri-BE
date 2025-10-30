package sandri.sandriweb.domain.place.entity;

import jakarta.persistence.*;
import lombok.*;
import sandri.sandriweb.domain.review.entity.PlaceReview;
import sandri.sandriweb.domain.review.entity.PlaceReviewPhoto;
import sandri.sandriweb.global.entity.BaseEntity;
import org.locationtech.jts.geom.*;

import java.util.List;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "places")
public class Place extends BaseEntity {

    @Id
    @Column(name = "place_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location", nullable = false)
    private Point location;

    @Column(name = "phone", length = 12)
    private String phone;

    @Column(name = "webpage")
    private String webpage;

    @Column(name = "summery")
    private String summery;

    @Column(name = "inform")
    private String information;

    @OneToMany(mappedBy = "place")
    List<PlacePhoto> photos;

    @OneToMany(mappedBy = "place")
    List<PlaceReview> reviews;

    @OneToMany(mappedBy = "place")
    List<PlaceReviewPhoto> reviewPhotos;
}
