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
}
