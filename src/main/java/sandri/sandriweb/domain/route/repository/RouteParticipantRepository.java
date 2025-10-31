package sandri.sandriweb.domain.route.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.entity.RouteParticipant;
import sandri.sandriweb.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface RouteParticipantRepository extends JpaRepository<RouteParticipant, Long> {
    
    List<RouteParticipant> findByRoute(Route route);
    
    Optional<RouteParticipant> findByRouteAndUser(Route route, User user);
    
    boolean existsByRouteAndUser(Route route, User user);
    
    void deleteByRouteAndUser(Route route, User user);
    
    @Query("SELECT COUNT(p) FROM RouteParticipant p WHERE p.route = :route")
    long countByRoute(@Param("route") Route route);
}

