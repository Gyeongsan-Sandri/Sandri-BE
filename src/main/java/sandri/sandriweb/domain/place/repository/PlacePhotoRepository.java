package sandri.sandriweb.domain.place.repository;

import org.springframework.data.domain.Pageable;
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
     * 여러 장소의 첫 번째 사진 정보 조회 (place_id와 photo_url만)
     * N+1 문제 방지를 위해 배치 조회 사용
     * 윈도우 함수를 사용하여 한 번의 테이블 스캔으로 처리 (효율적)
     * @param placeIds 장소 ID 목록
     * @return [placeId, photoUrl] 형태의 Object[] 리스트
     */
    @Query(value = "SELECT pp.place_id, pp.photo_url FROM ( " +
           "    SELECT pp.place_id, pp.photo_url, " +
           "           ROW_NUMBER() OVER (PARTITION BY pp.place_id ORDER BY pp.`order` ASC) as rn " +
           "    FROM place_photos pp " +
           "    WHERE pp.place_id IN :placeIds " +
           "    AND pp.enabled = true " +
           ") pp WHERE pp.rn = 1", nativeQuery = true)
    List<Object[]> findFirstPhotoUrlByPlaceIdIn(@Param("placeIds") List<Long> placeIds);
    
    /*
     * 여러 장소의 첫 번째 사진 정보 조회 (커서 기반 페이징)
     * place_id를 커서로 사용하여 배치 처리
     * 윈도우 함수를 사용하여 한 번의 테이블 스캔으로 처리 (효율적)
     * @param lastPlaceId 마지막으로 조회한 place_id (첫 조회시 null)
     * @param pageable 페이지 정보 (size + 1로 조회하여 hasNext 판단)
     * @return [placeId, photoUrl] 형태의 Object[] 리스트
     */
    @Query(value = "SELECT pp.place_id, pp.photo_url FROM ( " +
           "    SELECT pp.place_id, pp.photo_url, " +
           "           ROW_NUMBER() OVER (PARTITION BY pp.place_id ORDER BY pp.`order` ASC) as rn " +
           "    FROM place_photos pp " +
           "    WHERE pp.enabled = true " +
           "    AND (:lastPlaceId IS NULL OR pp.place_id > :lastPlaceId) " +
           ") pp WHERE pp.rn = 1 " +
           "ORDER BY pp.place_id ASC", nativeQuery = true)
    List<Object[]> findFirstPhotoUrlByPlaceIdWithCursor(@Param("lastPlaceId") Long lastPlaceId, Pageable pageable);
    
    /*
     * 여러 장소의 첫 번째 사진 정보 조회 (개수 제한)
     * 윈도우 함수를 사용하여 한 번의 테이블 스캔으로 처리 (효율적)
     * @param limit 조회할 개수
     * @return [placeId, photoUrl] 형태의 Object[] 리스트
     */
    @Query(value = "SELECT pp.place_id, pp.photo_url FROM ( " +
           "    SELECT pp.place_id, pp.photo_url, " +
           "           ROW_NUMBER() OVER (PARTITION BY pp.place_id ORDER BY pp.`order` ASC) as rn " +
           "    FROM place_photos pp " +
           "    WHERE pp.enabled = true " +
           ") pp WHERE pp.rn = 1 " +
           "ORDER BY pp.place_id ASC " +
           "LIMIT :limit", nativeQuery = true)
    List<Object[]> findFirstPhotoUrlByPlaceIdLimit(@Param("limit") int limit);
    
    /*
     * 특정 장소의 최대 order 값 조회 (새 사진 추가 시 order 계산용)
     */
    @Query("SELECT COALESCE(MAX(pp.order), -1) FROM PlacePhoto pp WHERE pp.place.id = :placeId AND pp.enabled = true")
    Integer findMaxOrderByPlaceId(@Param("placeId") Long placeId);
    
}

