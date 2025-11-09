package sandri.sandriweb.domain.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.search.entity.RecentSearch;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {

    /**
     * 사용자의 최근 검색어 목록 조회 (최신순, 최대 10개)
     */
    @Query("SELECT rs FROM RecentSearch rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.enabled = true " +
           "ORDER BY rs.createdAt DESC")
    List<RecentSearch> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 특정 키워드로 최근 검색어 조회
     */
    Optional<RecentSearch> findByUserIdAndKeywordAndSearchTypeAndEnabledTrue(
            Long userId, String keyword, RecentSearch.SearchType searchType);

    /**
     * 사용자의 모든 최근 검색어 삭제
     */
    @Modifying
    @Query("UPDATE RecentSearch rs SET rs.enabled = false WHERE rs.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}

