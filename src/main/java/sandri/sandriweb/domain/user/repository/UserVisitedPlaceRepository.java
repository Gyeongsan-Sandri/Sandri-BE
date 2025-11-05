package sandri.sandriweb.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.user.entity.UserVisitedPlace;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVisitedPlaceRepository extends JpaRepository<UserVisitedPlace, Long> {

    /**
     * 특정 사용자가 특정 장소를 방문했는지 확인
     */
    Optional<UserVisitedPlace> findByUserIdAndPlaceId(Long userId, Long placeId);

    /**
     * 사용자의 방문 장소 목록을 방문 날짜 내림차순으로 조회
     */
    List<UserVisitedPlace> findByUserIdOrderByVisitDateDesc(Long userId);
}

