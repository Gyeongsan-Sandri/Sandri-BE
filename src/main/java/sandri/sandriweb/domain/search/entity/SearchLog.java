package sandri.sandriweb.domain.search.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.global.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "search_logs", indexes = {
    @Index(name = "idx_keyword_created_at", columnList = "keyword, created_at DESC"),
    @Index(name = "idx_created_at", columnList = "created_at DESC")
})
public class SearchLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_log_id")
    private Long id;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "search_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SearchType searchType; // PLACE, ROUTE

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    public enum SearchType {
        PLACE, ROUTE
    }
}

