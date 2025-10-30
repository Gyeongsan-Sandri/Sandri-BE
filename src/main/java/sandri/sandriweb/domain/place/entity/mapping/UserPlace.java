package sandri.sandriweb.domain.place.entity.mapping;

import jakarta.persistence.*;
import lombok.*;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.global.entity.BaseEntity;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "place_likes")
public class UserPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_likes_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;
}
