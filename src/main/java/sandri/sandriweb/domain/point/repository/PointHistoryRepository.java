package sandri.sandriweb.domain.point.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.point.entity.PointHistory;

import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    /**
     * 특정 사용자의 포인트 히스토리 조회 (최신순)
     * @param userId 사용자 ID
     * @return 포인트 히스토리 리스트
     */
    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
