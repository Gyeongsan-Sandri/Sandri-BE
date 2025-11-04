package sandri.sandriweb.domain.place.repository;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.place.entity.Place;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    // 근처 장소 조회 (반경 내)
    // 참고: ST_Distance_Sphere는 MySQL/MariaDB용 함수입니다.
    // PostgreSQL을 사용하는 경우 ST_Distance와 ST_Transform을 사용하거나,
    // PostGIS의 ST_DistanceSphere 함수를 사용해야 합니다.
    @Query(value = "SELECT p.* FROM places p " +
            "WHERE ST_Distance_Sphere(p.location, :center) <= :radius " +
            "AND p.place_id != :excludeId " +
            "AND p.enabled = true " +
            "ORDER BY ST_Distance_Sphere(p.location, :center) " +
            "LIMIT :limit", nativeQuery = true)
    List<Place> findNearbyPlaces(@Param("center") Point center, 
                                  @Param("radius") double radius, 
                                  @Param("excludeId") Long excludeId,
                                  @Param("limit") int limit);
    
    // 카테고리별 근처 장소 조회 (반경 내) - group 기준 (관광지/맛집/카페)
    @Query(value = "SELECT p.* FROM places p " +
            "WHERE ST_Distance_Sphere(p.location, :center) <= :radius " +
            "AND p.place_id != :excludeId " +
            "AND p.enabled = true " +
            "AND p.`group` = :categoryName " +
            "ORDER BY ST_Distance_Sphere(p.location, :center) " +
            "LIMIT :limit", nativeQuery = true)
    List<Place> findNearbyPlacesByCategory(@Param("center") Point center,
                                           @Param("radius") double radius,
                                           @Param("excludeId") Long excludeId,
                                           @Param("categoryName") String categoryName,
                                           @Param("limit") int limit);

    /**
     * 카테고리별 장소 조회 (좋아요 많은 순)
     * @param categoryName 카테고리 이름 (자연_힐링, 역사_전통, 문화_체험, 식도락)
     * @param limit 조회할 개수
     * @return 좋아요 많은 순으로 정렬된 장소 리스트
     */
    @Query(value = "SELECT p.* FROM places p " +
           "LEFT JOIN (SELECT place_id, COUNT(*) as like_count " +
           "           FROM place_likes " +
           "           WHERE enabled = true " +
           "           GROUP BY place_id) AS likes ON p.place_id = likes.place_id " +
           "WHERE p.category = :categoryName " +
           "AND p.enabled = true " +
           "ORDER BY COALESCE(likes.like_count, 0) DESC, p.created_at DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<Place> findByCategoryOrderByLikeCountDesc(@Param("categoryName") String categoryName,
                                                    @Param("limit") int limit);
}
