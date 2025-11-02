package sandri.sandriweb.domain.advertise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.advertise.entity.OfficialAd;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfficialAdRepository extends JpaRepository<OfficialAd, Long> {

    /**
     * 유효한 공식 광고 조회
     * - enabled = true
     * - 현재 시간이 startDate와 endDate 사이
     * - displayOrder로 정렬
     */
    @Query("SELECT a FROM OfficialAd a " +
           "WHERE a.enabled = true " +
           "AND (a.startDate IS NULL OR a.startDate <= :now) " +
           "AND (a.endDate IS NULL OR a.endDate >= :now) " +
           "ORDER BY a.displayOrder ASC, a.createdAt DESC")
    List<OfficialAd> findValidAds(@Param("now") LocalDateTime now);
}

