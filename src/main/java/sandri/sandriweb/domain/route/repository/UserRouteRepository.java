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
}

