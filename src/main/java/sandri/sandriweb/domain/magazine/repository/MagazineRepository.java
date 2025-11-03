package sandri.sandriweb.domain.magazine.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.magazine.entity.Magazine;

import java.util.List;
import java.util.Optional;

@Repository
public interface MagazineRepository extends JpaRepository<Magazine, Long> {

    /**
     * Magazine과 MagazineCard를 함께 조회 (FETCH JOIN)
     * @param magazineId 매거진 ID
     * @return Magazine (cards 포함, enabled 필터링은 Java에서 처리)
     */
    @Query("SELECT DISTINCT m FROM Magazine m " +
           "LEFT JOIN FETCH m.cards c " +
           "WHERE m.id = :magazineId " +
           "AND m.enabled = true")
    Optional<Magazine> findByIdWithCards(@Param("magazineId") Long magazineId);

    /**
     * enabled된 매거진 목록 조회 (최신순)
     * @param pageable 페이징 정보
     * @return 매거진 목록
     */
    @Query("SELECT DISTINCT m FROM Magazine m " +
           "WHERE m.enabled = true " +
           "ORDER BY m.createdAt DESC")
    List<Magazine> findEnabledMagazinesOrderByCreatedAtDesc(Pageable pageable);

    /**
     * enabled된 매거진 목록 조회 (커서 기반 페이징 - 최신순)
     * @param lastMagazineId 마지막으로 조회한 매거진 ID (첫 조회시 null)
     * @param pageable 페이징 정보
     * @return 매거진 목록
     */
    @Query("SELECT DISTINCT m FROM Magazine m " +
           "WHERE m.enabled = true " +
           "AND (:lastMagazineId IS NULL OR " +
           "     m.createdAt < (SELECT m2.createdAt FROM Magazine m2 WHERE m2.id = :lastMagazineId) OR " +
           "     (m.createdAt = (SELECT m2.createdAt FROM Magazine m2 WHERE m2.id = :lastMagazineId) AND m.id < :lastMagazineId)) " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<Magazine> findEnabledMagazinesOrderByCreatedAtDescWithCursor(
            @Param("lastMagazineId") Long lastMagazineId,
            Pageable pageable);
}

