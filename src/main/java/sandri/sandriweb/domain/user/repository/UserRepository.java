package sandri.sandriweb.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.user.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByNickname(String nickname);
    
    boolean existsByUsername(String username);
    
    boolean existsByNickname(String nickname);
    
    /**
     * username으로 userId만 조회 (성능 최적화용)
     */
    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u WHERE u.username = :username")
    Optional<Long> findUserIdByUsername(@org.springframework.data.repository.query.Param("username") String username);
}
