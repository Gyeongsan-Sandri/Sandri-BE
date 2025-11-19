package sandri.sandriweb.domain.user.entity;

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
@Table(name = "travel_style_places", uniqueConstraints = {
    @UniqueConstraint(name = "uc_travel_style_place", columnNames = {"travel_style", "place_id"})
})
public class TravelStylePlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_style_place_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style", nullable = false, length = 50)
    private User.TravelStyle travelStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;
}

