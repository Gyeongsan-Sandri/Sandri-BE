package sandri.sandriweb.domain.magazine.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.magazine.entity.mapping.MagazineTag;
import sandri.sandriweb.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "tags")
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY)
    private List<MagazineTag> magazineTags = new ArrayList<>();

    /**
     * 태그 이름 수정
     * @param name 태그 이름
     */
    public void update(String name) {
        this.name = name;
    }
}

