package sandri.sandriweb.domain.place.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.place.entity.mapping.UserPlace;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPlaceRepository extends JpaRepository<UserPlace, Long> {

    /**
     * 특정 사용자가 특정 장소에 좋아요를 눌렀는지 확인
     */
    Optional<UserPlace> findByUserIdAndPlaceId(Long userId, Long placeId);

    /**
     * 여러 장소의 좋아요 수를 한 번에 조회
     */
    @Query("SELECT up.place.id, COUNT(up.id) " +
           "FROM UserPlace up " +
           "WHERE up.place.id IN :placeIds " +
           "AND up.enabled = true " +
           "GROUP BY up.place.id")
    List<Object[]> countLikesByPlaceIds(@Param("placeIds") List<Long> placeIds);

    /**
     * 특정 사용자가 좋아요한 장소 ID 목록 조회
     */
    @Query("SELECT up.place.id " +
           "FROM UserPlace up " +
           "WHERE up.user.id = :userId " +
           "AND up.place.id IN :placeIds " +
           "AND up.enabled = true")
    List<Long> findLikedPlaceIdsByUserId(@Param("userId") Long userId, @Param("placeIds") List<Long> placeIds);

    /**
     * 특정 사용자가 좋아요한 장소 목록 조회 (최신순)
     */
    @Query("SELECT up.place " +
           "FROM UserPlace up " +
           "WHERE up.user.id = :userId " +
           "AND up.enabled = true " +
           "ORDER BY up.updatedAt DESC")
    List<sandri.sandriweb.domain.place.entity.Place> findLikedPlacesByUserId(@Param("userId") Long userId);
}

