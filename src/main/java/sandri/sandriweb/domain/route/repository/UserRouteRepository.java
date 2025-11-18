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
     * 최근 기간 가중치 기반 HOT 루트 집계 (공개 루트만)
     */
    @Query(value = "SELECT ur.route_id AS routeId, " +
            "COUNT(*) AS totalLikes, " +
            "SUM(CASE WHEN ur.updated_at >= DATE_SUB(NOW(), INTERVAL :recentDays DAY) THEN 1 ELSE 0 END) AS recentLikes " +
            "FROM route_likes ur " +
            "JOIN routes r ON ur.route_id = r.id " +
            "WHERE ur.enabled = true AND r.is_public = true " +
            "GROUP BY ur.route_id " +
            "ORDER BY (COUNT(*) + :alpha * SUM(CASE WHEN ur.updated_at >= DATE_SUB(NOW(), INTERVAL :recentDays DAY) THEN 1 ELSE 0 END)) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findHotRoutes(@Param("limit") int limit,
                                 @Param("recentDays") int recentDays,
                                 @Param("alpha") double alpha);
}

