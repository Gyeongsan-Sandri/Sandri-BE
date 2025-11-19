package sandri.sandriweb.domain.route.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.entity.UserRoute;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {

    Optional<UserRoute> findByUserIdAndRouteId(Long userId, Long routeId);

    @Query("SELECT ur.route " +
           "FROM UserRoute ur " +
           "WHERE ur.user.id = :userId " +
           "AND ur.enabled = true " +
           "ORDER BY ur.updatedAt DESC")
    List<Route> findLikedRoutesByUserId(@Param("userId") Long userId);

    @Query("SELECT ur FROM UserRoute ur WHERE ur.user.id = :userId AND ur.enabled = true")
    List<UserRoute> findAllEnabledByUserId(@Param("userId") Long userId);

    /**
     * 최근 기간 가중치 기반 HOT 루트 집계 (공개 루트만, 좋아요 0개인 루트도 포함)
     */
    @Query(value = "SELECT r.id AS routeId, " +
            "COALESCE(COUNT(ur.id), 0) AS totalLikes, " +
            "COALESCE(SUM(CASE WHEN ur.updated_at >= DATE_SUB(NOW(), INTERVAL :recentDays DAY) THEN 1 ELSE 0 END), 0) AS recentLikes " +
            "FROM routes r " +
            "LEFT JOIN route_likes ur ON r.id = ur.route_id AND ur.enabled = true " +
            "WHERE r.enabled = true AND r.is_public = true " +
            "GROUP BY r.id " +
            "ORDER BY (COALESCE(COUNT(ur.id), 0) + :alpha * COALESCE(SUM(CASE WHEN ur.updated_at >= DATE_SUB(NOW(), INTERVAL :recentDays DAY) THEN 1 ELSE 0 END), 0)) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findHotRoutes(@Param("limit") int limit,
                                 @Param("recentDays") int recentDays,
                                 @Param("alpha") double alpha);
}

