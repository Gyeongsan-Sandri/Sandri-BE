package sandri.sandriweb.domain.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.search.entity.SearchLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 최근 24시간 검색 로그 조회 (집계용)
     * @param startTime 시작 시간 (24시간 전)
     * @return 검색 로그 목록
     */
    @Query("SELECT sl FROM SearchLog sl " +
           "WHERE sl.searchedAt >= :startTime " +
           "AND sl.enabled = true " +
           "ORDER BY sl.searchedAt DESC")
    List<SearchLog> findRecentLogs(@Param("startTime") LocalDateTime startTime);

    /**
     * 최근 24시간 검색 키워드별 집계 (검색 횟수 기준)
     * @param startTime 시작 시간 (24시간 전)
     * @return [keyword, count] 형태의 Object[] 리스트
     */
    @Query("SELECT sl.keyword, COUNT(sl.id) as searchCount " +
           "FROM SearchLog sl " +
           "WHERE sl.searchedAt >= :startTime " +
           "AND sl.enabled = true " +
           "GROUP BY sl.keyword " +
           "ORDER BY searchCount DESC, sl.keyword ASC")
    List<Object[]> countSearchesByKeyword(@Param("startTime") LocalDateTime startTime);
}

