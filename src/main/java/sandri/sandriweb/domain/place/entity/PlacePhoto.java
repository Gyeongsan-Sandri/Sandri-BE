package sandri.sandriweb.domain.place.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.global.entity.BaseEntity;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "place_photos")
public class PlacePhoto extends BaseEntity {

    @Id
    @Column(name = "place_photo_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "photo_url", nullable = false, length = 1000)
    private String photoUrl;
    
    @Column(name = "`order`", nullable = false)
    private Integer order; // 사진 순서 (0부터 시작)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    /**
     * 사진 URL 수정
     * @param photoUrl 사진 URL
     */
    public void updatePhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
