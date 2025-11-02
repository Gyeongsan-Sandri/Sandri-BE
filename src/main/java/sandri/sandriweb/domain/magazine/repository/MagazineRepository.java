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
     * @return Magazine (cards 포함)
     */
    @Query("SELECT DISTINCT m FROM Magazine m " +
           "LEFT JOIN FETCH m.cards c " +
           "WHERE m.id = :magazineId " +
           "AND m.enabled = true " +
           "AND (c.enabled IS NULL OR c.enabled = true) " +
           "ORDER BY c.createdAt ASC")
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
}

