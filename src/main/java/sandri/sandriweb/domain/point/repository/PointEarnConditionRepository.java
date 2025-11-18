package sandri.sandriweb.domain.point.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.point.entity.PointEarnCondition;
import sandri.sandriweb.domain.point.enums.ConditionType;

import java.util.Optional;

@Repository
public interface PointEarnConditionRepository extends JpaRepository<PointEarnCondition, Long> {

    /**
     * 조건 타입으로 적립 조건 조회
     * @param conditionType 조건 타입
     * @return 적립 조건 (Optional)
     */
    Optional<PointEarnCondition> findByConditionType(ConditionType conditionType);
}
