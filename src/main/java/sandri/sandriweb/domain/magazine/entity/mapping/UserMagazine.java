package sandri.sandriweb.domain.magazine.entity.mapping;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.magazine.entity.Magazine;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.global.entity.BaseEntity;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "magazine_likes")
public class UserMagazine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_likes_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;
}
