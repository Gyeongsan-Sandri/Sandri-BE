package sandri.sandriweb.domain.magazine.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.global.entity.BaseEntity;

import java.util.List;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "magazines")
public class Magazine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "magazine_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;


    @Column(name = "summary")
    private String summary;
    
    @Column(name = "content")
    private String content;

    @OneToMany(mappedBy = "magazine")
    List<MagazineCard> cards;
}
