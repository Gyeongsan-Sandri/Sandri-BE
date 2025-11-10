package sandri.sandriweb.domain.magazine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.magazine.entity.Tag;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    /**
     * 태그 이름으로 태그 조회
     * @param name 태그 이름
     * @return 태그 (없으면 Optional.empty())
     */
    Optional<Tag> findByName(String name);
    
    /**
     * 태그 이름으로 태그 존재 여부 확인
     * @param name 태그 이름
     * @return 존재 여부
     */
    boolean existsByName(String name);
    
    /**
     * enabled된 태그 목록 조회
     * @return enabled된 태그 목록
     */
    List<Tag> findByEnabledTrue();
}

