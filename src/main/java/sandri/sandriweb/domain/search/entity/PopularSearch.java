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
@Table(name = "popular_searches", indexes = {
    @Index(name = "idx_aggregated_at", columnList = "aggregated_at DESC")
})
public class PopularSearch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "popular_search_id")
    private Long id;

    @Column(name = "`rank`", nullable = false)
    private Integer rank;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private Long searchCount;

    @Column(name = "previous_rank")
    private Integer previousRank; // 이전 순위 (없으면 null)

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt; // 집계 시간
}

