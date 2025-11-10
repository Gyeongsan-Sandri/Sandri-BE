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

    /*
     * Magazine과 MagazineCard를 함께 조회 (FETCH JOIN)
     * @param magazineId 매거진 ID
     * @return Magazine (cards 포함, enabled 필터링 및 정렬은 DB에서 처리)
     */
    @Query("SELECT DISTINCT m FROM Magazine m " +
           "LEFT JOIN FETCH m.cards c " +
           "WHERE m.id = :magazineId " +
           "AND m.enabled = true " +
           "AND (c.enabled = true OR c IS NULL) " +
           "ORDER BY c.order ASC")
    Optional<Magazine> findByIdWithCards(@Param("magazineId") Long magazineId);

    /*
     * 커서 기반 페이징: 마지막 매거진 ID 이후의 목록 조회 (썸네일만 fetch)
     * id DESC 정렬 기준으로 커서 진행 (AUTO_INCREMENT이므로 순서 보장)
     * MultipleBagFetchException 방지를 위해 cards만 fetch (tags는 별도 쿼리로 조회)
     */
    @Query("SELECT DISTINCT m FROM Magazine m " +
           "LEFT JOIN FETCH m.cards c " +
           "WHERE m.enabled = true " +
           "AND (c.order = 0 OR c IS NULL) " +
           "AND (c.enabled = true OR c IS NULL) " +
           "AND (:lastId IS NULL OR m.id < :lastId) " +
           "ORDER BY m.id DESC")
    List<Magazine> findEnabledWithThumbnailByCursor(@Param("lastId") Long lastId, Pageable pageable);

    /**
     * enabled된 매거진 총 개수 조회
     */
    long countByEnabledTrue();

    /**
     * 매거진 이름으로 존재 여부 확인 (중복 검사용)
     * @param name 매거진 이름
     * @return 존재 여부
     */
    boolean existsByName(String name);

    /*
     * Magazine과 MagazineCard, Place를 함께 조회 (FETCH JOIN)
     * @param magazineId 매거진 ID
     * @return Magazine (cards와 각 card의 place 포함, enabled 필터링 및 정렬은 DB에서 처리)
     */
    @Query("SELECT DISTINCT m FROM Magazine m " +
           "LEFT JOIN FETCH m.cards c " +
           "LEFT JOIN FETCH c.place p " +
           "WHERE m.id = :magazineId " +
           "AND m.enabled = true " +
           "AND (c.enabled = true OR c IS NULL) " +
           "AND (p.enabled = true OR p IS NULL) " +
           "ORDER BY c.order ASC")
    Optional<Magazine> findByIdWithCardsAndPlaces(@Param("magazineId") Long magazineId);
}

