package sandri.sandriweb.domain.magazine.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.global.entity.BaseEntity;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "magazine_cards")
public class MagazineCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "magazine_card_id")
    private Long id;

    @Column(name = "`order`", nullable = false)
    private Integer order; // 카드 순서 (0부터 시작)
    
    @Column(name = "card_url", nullable = false)
    private String cardUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;
}
