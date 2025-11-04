package sandri.sandriweb.domain.magazine.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.global.entity.BaseEntity;

import java.util.ArrayList;
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

    @OneToMany(mappedBy = "magazine", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    List<MagazineCard> cards = new ArrayList<>();

    /*
     * 매거진 정보 수정
     * @param name 매거진 이름
     * @param summary 매거진 요약
     * @param content 매거진 내용
     */
    public void update(String name, String summary, String content) {
        this.name = name;
        this.summary = summary;
        this.content = content;
    }
}
