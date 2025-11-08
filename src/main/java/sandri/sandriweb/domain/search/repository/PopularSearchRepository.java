package sandri.sandriweb.domain.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.search.entity.PopularSearch;

import java.util.List;

@Repository
public interface PopularSearchRepository extends JpaRepository<PopularSearch, Long> {

    /**
     * 최신 집계 결과 조회 (상위 5개)
     * @return 최신 인기 검색어 목록
     */
    @Query("SELECT ps FROM PopularSearch ps " +
           "WHERE ps.enabled = true " +
           "ORDER BY ps.aggregatedAt DESC, ps.rank ASC")
    List<PopularSearch> findLatestPopularSearches();

    /**
     * 이전 집계 결과 조회 (순위 변동 계산용)
     * @return 이전 인기 검색어 목록
     */
    @Query("SELECT ps FROM PopularSearch ps " +
           "WHERE ps.enabled = true " +
           "AND ps.aggregatedAt < (SELECT MAX(ps2.aggregatedAt) FROM PopularSearch ps2 WHERE ps2.enabled = true) " +
           "ORDER BY ps.aggregatedAt DESC, ps.rank ASC " +
           "LIMIT 5")
    List<PopularSearch> findPreviousPopularSearches();

    /**
     * 이전 집계 결과 비활성화 (새 집계 결과 저장 전)
     */
    @Modifying
    @Query("UPDATE PopularSearch ps SET ps.enabled = false WHERE ps.enabled = true")
    void disableAllPopularSearches();
}

