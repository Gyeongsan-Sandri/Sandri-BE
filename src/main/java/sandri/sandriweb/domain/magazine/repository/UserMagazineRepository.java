package sandri.sandriweb.domain.magazine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.magazine.entity.mapping.UserMagazine;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMagazineRepository extends JpaRepository<UserMagazine, Long> {

    /**
     * 특정 사용자가 특정 매거진에 좋아요를 눌렀는지 확인
     * enabled 여부와 관계없이 조회 (토글 로직에서 disabled된 좋아요도 재활성화 가능)
     * @param userId 사용자 ID
     * @param magazineId 매거진 ID
     * @return UserMagazine (없으면 Optional.empty())
     */
    Optional<UserMagazine> findByUserIdAndMagazineId(Long userId, Long magazineId);

    /**
     * 특정 사용자가 좋아요한 매거진 ID 목록 조회
     */
    @Query("SELECT um.magazine.id " +
           "FROM UserMagazine um " +
           "WHERE um.user.id = :userId " +
           "AND um.magazine.id IN :magazineIds " +
           "AND um.enabled = true")
    List<Long> findLikedMagazineIdsByUserId(@Param("userId") Long userId, @Param("magazineIds") List<Long> magazineIds);
}

