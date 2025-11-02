package sandri.sandriweb.domain.route.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    
    List<Route> findByCreator(User creator);
    
    List<Route> findByCreatorAndIsPublic(User creator, boolean isPublic);
    
    Optional<Route> findByShareCode(String shareCode);
    
    @Query("SELECT r FROM Route r JOIN r.participants p WHERE p.user = :user OR r.creator = :user")
    List<Route> findByParticipantOrCreator(@Param("user") User user);
    
    boolean existsByIdAndCreator(Long id, User creator);
}

