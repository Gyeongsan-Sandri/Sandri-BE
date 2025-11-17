package sandri.sandriweb.domain.place.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.review.entity.PlaceReview;
import sandri.sandriweb.domain.review.entity.PlaceReviewPhoto;
import sandri.sandriweb.domain.place.enums.PlaceCategory;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.global.entity.BaseEntity;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "places", uniqueConstraints = {
    @UniqueConstraint(name = "uc_place_name_address", columnNames = {"name", "address"})
})
public class Place extends BaseEntity {

    @Id
    @Column(name = "place_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address; // 한글 주소 (외부 API로 조회하여 저장)

    @Column(name = "location", nullable = false)
    private Point location;

    @Column(name = "summery", length = 1000)
    private String summery;

    @Column(name = "inform", length = 2000)
    private String information;

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private List<PlacePhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY)
    private List<PlaceReview> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY)
    private List<PlaceReviewPhoto> reviewPhotos = new ArrayList<>();

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private List<PlaceOpenTime> openTimes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "`group`", nullable = false)
    private PlaceCategory group; // 관광지/맛집/카페

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category; // 자연/힐링, 역사/전통, 문화/체험, 식도락

    // 위도, 경도 추출 헬퍼 메서드
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }

    // 장소 정보 업데이트 메서드
    public void update(String name, String address, Point location, String summary, 
                       String information, PlaceCategory group, Category category) {
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (location != null) {
            this.location = location;
        }
        if (summary != null) {
            this.summery = summary;
        }
        if (information != null) {
            this.information = information;
        }
        if (group != null) {
            this.group = group;
        }
        if (category != null) {
            this.category = category;
        }
    }
}
