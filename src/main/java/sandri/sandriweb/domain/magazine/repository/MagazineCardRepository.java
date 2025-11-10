package sandri.sandriweb.domain.magazine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.magazine.entity.MagazineCard;

import java.util.Optional;

@Repository
public interface MagazineCardRepository extends JpaRepository<MagazineCard, Long> {
    
    /**
     * 카드 URL로 존재 여부 확인 (중복 검사용)
     * @param cardUrl 카드 이미지 URL
     * @return 존재 여부
     */
    boolean existsByCardUrl(String cardUrl);
    
    /**
     * 매거진 ID와 order로 enabled된 카드 조회
     * @param magazineId 매거진 ID
     * @param order 카드 순서
     * @return MagazineCard (enabled된 것만)
     */
    @Query("SELECT c FROM MagazineCard c WHERE c.magazine.id = :magazineId AND c.order = :order AND c.enabled = true")
    Optional<MagazineCard> findByMagazineIdAndOrder(@Param("magazineId") Long magazineId, @Param("order") Integer order);
}
