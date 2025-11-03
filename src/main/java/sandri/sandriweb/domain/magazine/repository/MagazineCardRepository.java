package sandri.sandriweb.domain.magazine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.magazine.entity.MagazineCard;

import java.util.List;

@Repository
public interface MagazineCardRepository extends JpaRepository<MagazineCard, Long> {
    
    /**
     * 매거진 ID로 카드 목록 조회
     * @param magazineId 매거진 ID
     * @return 카드 목록 (order 순으로 정렬)
     */
    @Query("SELECT c FROM MagazineCard c WHERE c.magazine.id = :magazineId ORDER BY c.order ASC")
    List<MagazineCard> findByMagazineId(@Param("magazineId") Long magazineId);
}

