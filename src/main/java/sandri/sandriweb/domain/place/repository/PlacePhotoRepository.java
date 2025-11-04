package sandri.sandriweb.domain.place.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.place.entity.PlacePhoto;

import java.util.List;

@Repository
public interface PlacePhotoRepository extends JpaRepository<PlacePhoto, Long> {
    
    /*
     * 장소 ID로 사진 목록 조회 (enabled된 것만)
     * @param placeId 장소 ID
     * @return 사진 목록
     */
    @Query("SELECT pp FROM PlacePhoto pp WHERE pp.place.id = :placeId AND pp.enabled = true ORDER BY pp.order ASC")
    List<PlacePhoto> findByPlaceId(@Param("placeId") Long placeId);
    
    /*
     * 여러 장소의 첫 번째 사진만 조회 (각 장소당 한 장씩)
     * 윈도우 함수를 사용하여 더 효율적으로 개선
     * @param placeIds 장소 ID 목록
     * @return 각 장소의 첫 번째 사진 목록
     */
    @Query(value = "SELECT pp.* FROM ( " +
           "    SELECT pp.*, " +
           "           ROW_NUMBER() OVER (PARTITION BY pp.place_id ORDER BY pp.`order` ASC) as rn " +
           "    FROM place_photos pp " +
           "    WHERE pp.place_id IN :placeIds " +
           "    AND pp.enabled = true " +
           ") pp WHERE pp.rn = 1", nativeQuery = true)
    List<PlacePhoto> findFirstPhotoByPlaceIdIn(@Param("placeIds") List<Long> placeIds);
    
    /*
     * 특정 장소의 최대 order 값 조회 (새 사진 추가 시 order 계산용)
     */
    @Query("SELECT COALESCE(MAX(pp.order), -1) FROM PlacePhoto pp WHERE pp.place.id = :placeId AND pp.enabled = true")
    Integer findMaxOrderByPlaceId(@Param("placeId") Long placeId);
    
}

