package sandri.sandriweb.domain.place.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.place.entity.PlacePhoto;

import java.util.List;

@Repository
public interface PlacePhotoRepository extends JpaRepository<PlacePhoto, Long> {
    
    List<PlacePhoto> findByPlaceId(Long placeId);
    
    /**
     * 여러 장소의 첫 번째 사진만 조회 (각 장소당 한 장씩)
     * @param placeIds 장소 ID 목록
     * @return 각 장소의 첫 번째 사진 목록
     */
    @Query(value = "SELECT pp1.* FROM place_photos pp1 " +
           "INNER JOIN ( " +
           "    SELECT place_id, MIN(place_photo_id) as min_id " +
           "    FROM place_photos " +
           "    WHERE place_id IN :placeIds " +
           "    AND enabled = true " +
           "    GROUP BY place_id " +
           ") pp2 ON pp1.place_id = pp2.place_id AND pp1.place_photo_id = pp2.min_id " +
           "WHERE pp1.enabled = true", nativeQuery = true)
    List<PlacePhoto> findFirstPhotoByPlaceIdIn(@Param("placeIds") List<Long> placeIds);
    
}

