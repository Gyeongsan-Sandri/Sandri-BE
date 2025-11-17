package sandri.sandriweb.domain.visitHistory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.visitHistory.entity.mapping.UserPlaceHistory;

import java.util.List;

@Repository
public interface UserPlaceHistoryRepository extends JpaRepository<UserPlaceHistory, Long> {

    /**
     * 특정 사용자의 모든 방문 기록 조회
     * @param userId 사용자 ID
     * @return 해당 사용자의 모든 방문 기록 리스트
     */
    List<UserPlaceHistory> findByUserId(Long userId);

    /**
     * 특정 사용자의 모든 방문 기록 조회 (Place와 PlacePhoto를 함께 조회하여 N+1 문제 해결)
     * @param userId 사용자 ID
     * @return 해당 사용자의 모든 방문 기록 리스트 (Place, PlacePhoto 포함)
     */
    @Query("SELECT DISTINCT h FROM UserPlaceHistory h " +
           "JOIN FETCH h.place p " +
           "LEFT JOIN FETCH p.photos " +
           "WHERE h.user.id = :userId " +
           "ORDER BY h.createdAt DESC")
    List<UserPlaceHistory> findByUserIdWithPlaceAndPhotos(@Param("userId") Long userId);
}
