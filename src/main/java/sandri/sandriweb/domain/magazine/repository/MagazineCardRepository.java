package sandri.sandriweb.domain.magazine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.magazine.entity.MagazineCard;

@Repository
public interface MagazineCardRepository extends JpaRepository<MagazineCard, Long> {
    
    /**
     * 카드 URL로 존재 여부 확인 (중복 검사용)
     * @param cardUrl 카드 이미지 URL
     * @return 존재 여부
     */
    boolean existsByCardUrl(String cardUrl);
}
