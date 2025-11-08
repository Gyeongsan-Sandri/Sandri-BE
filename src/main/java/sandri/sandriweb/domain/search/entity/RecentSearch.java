package sandri.sandriweb.domain.search.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.global.entity.BaseEntity;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "recent_searches", indexes = {
    @Index(name = "idx_user_id_created_at", columnList = "user_id, created_at DESC")
})
public class RecentSearch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recent_search_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "search_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SearchType searchType; // PLACE, ROUTE

    public enum SearchType {
        PLACE, ROUTE
    }
}

