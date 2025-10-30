package sandri.sandriweb.domain.magazine.entity;

import jakarta.persistence.*;
import lombok.*;
import sandri.sandriweb.global.entity.BaseEntity;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "magazine_cards")
public class MagazineCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "magazine_card_id")
    private Long id;

    @Column(name = "card_url", nullable = false)
    private String cardUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;
}
