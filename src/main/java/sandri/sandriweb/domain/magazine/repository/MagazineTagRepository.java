package sandri.sandriweb.domain.magazine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.magazine.entity.mapping.MagazineTag;

import java.util.List;

@Repository
public interface MagazineTagRepository extends JpaRepository<MagazineTag, Long> {
    
    /**
     * 매거진 ID와 태그 ID로 MagazineTag 존재 여부 확인
     * @param magazineId 매거진 ID
     * @param tagId 태그 ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(mt) > 0 FROM MagazineTag mt WHERE mt.magazine.id = :magazineId AND mt.tag.id = :tagId AND mt.enabled = true")
    boolean existsByMagazineIdAndTagId(@Param("magazineId") Long magazineId, @Param("tagId") Long tagId);
    
    /**
     * 여러 매거진의 태그를 한 번에 조회 (N+1 문제 방지)
     * @param magazineIds 매거진 ID 목록
     * @return MagazineTag 목록 (tag 포함)
     */
    @Query("SELECT mt FROM MagazineTag mt " +
           "LEFT JOIN FETCH mt.tag t " +
           "WHERE mt.magazine.id IN :magazineIds " +
           "AND mt.enabled = true " +
           "AND (t.enabled = true OR t IS NULL)")
    List<MagazineTag> findByMagazineIdInWithTag(@Param("magazineIds") List<Long> magazineIds);
    
    /**
     * 매거진 ID와 태그 ID로 MagazineTag 조회
     * @param magazineId 매거진 ID
     * @param tagId 태그 ID
     * @return MagazineTag (enabled된 것만)
     */
    @Query("SELECT mt FROM MagazineTag mt WHERE mt.magazine.id = :magazineId AND mt.tag.id = :tagId AND mt.enabled = true")
    java.util.Optional<MagazineTag> findByMagazineIdAndTagId(@Param("magazineId") Long magazineId, @Param("tagId") Long tagId);
}

