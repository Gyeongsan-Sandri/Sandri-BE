package sandri.sandriweb.domain.place.repository;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.enums.Category;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByName(String name);

    /**
     * 이름과 주소로 중복 체크
     * @param name 장소 이름
     * @param address 주소
     * @return 존재 여부
     */
    boolean existsByNameAndAddress(String name, String address);

    /**
     * 이름과 주소로 장소 조회
     * @param name 장소 이름
     * @param address 주소
     * @return 장소 (없으면 Optional.empty())
     */
    java.util.Optional<Place> findByNameAndAddress(String name, String address);

    /**
     * 이름으로 장소 조회
     * @param name 장소 이름
     * @return 장소 (없으면 Optional.empty())
     */
    java.util.Optional<Place> findByName(String name);

    /**
     * 여러 이름으로 장소 일괄 조회 (N+1 문제 방지)
     * @param names 장소 이름 목록
     * @return 이름을 키로 하는 장소 Map
     */
    @Query("SELECT p FROM Place p WHERE p.name IN :names AND p.enabled = true")
    List<Place> findByNameIn(@Param("names") java.util.Set<String> names);

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
    
    // 근처 장소 조회 (반경 제한 없음, 가까운 순으로 정렬, 현재 장소 포함)
    // 참고: ST_Distance_Sphere는 MySQL/MariaDB용 함수입니다.
    @Query(value = "SELECT p.* FROM places p " +
            "WHERE p.enabled = true " +
            "AND p.location IS NOT NULL " +
            "ORDER BY ST_Distance_Sphere(p.location, :center) ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Place> findNearestPlaces(@Param("center") Point center,
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
    
    // 대분류별 근처 장소 조회 (반경 내, 좋아요 많은 순) - group 기준 (관광지/맛집/카페)
    @Query(value = "SELECT p.* FROM places p " +
            "LEFT JOIN (SELECT place_id, COUNT(*) as like_count " +
            "           FROM place_likes " +
            "           WHERE enabled = true " +
            "           GROUP BY place_id) AS likes ON p.place_id = likes.place_id " +
            "WHERE ST_Distance_Sphere(p.location, :center) <= :radius " +
            "AND p.place_id != :excludeId " +
            "AND p.enabled = true " +
            "AND p.`group` = :groupName " +
            "ORDER BY COALESCE(likes.like_count, 0) DESC, p.created_at DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Place> findNearbyPlacesByGroupOrderByLikeCount(@Param("center") Point center,
                                                         @Param("radius") double radius,
                                                         @Param("excludeId") Long excludeId,
                                                         @Param("groupName") String groupName,
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
           "ORDER BY COALESCE(likes.like_count, 0) DESC, p.created_at DESC, p.place_id DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<Place> findByCategoryOrderByLikeCountDesc(@Param("categoryName") String categoryName,
                                                    @Param("limit") int limit);

    /**
     * 카테고리별 장소 조회 (커서 기반 페이징, 좋아요 많은 순)
     * @param categoryName 카테고리 이름
     * @param lastLikeCount 마지막 장소의 좋아요 수
     * @param lastCreatedAt 마지막 장소의 생성 시간
     * @param lastPlaceId 마지막 장소 ID
     * @param limit 조회할 개수
     * @return 좋아요 많은 순으로 정렬된 장소 리스트 (커서 이후)
     */
    @Query(value = "SELECT p.* FROM places p " +
           "LEFT JOIN (SELECT place_id, COUNT(*) as like_count " +
           "           FROM place_likes " +
           "           WHERE enabled = true " +
           "           GROUP BY place_id) AS likes ON p.place_id = likes.place_id " +
           "WHERE p.category = :categoryName " +
           "AND p.enabled = true " +
           "AND (COALESCE(likes.like_count, 0) < :lastLikeCount " +
           "     OR (COALESCE(likes.like_count, 0) = :lastLikeCount AND p.created_at < :lastCreatedAt) " +
           "     OR (COALESCE(likes.like_count, 0) = :lastLikeCount AND p.created_at = :lastCreatedAt AND p.place_id < :lastPlaceId)) " +
           "ORDER BY COALESCE(likes.like_count, 0) DESC, p.created_at DESC, p.place_id DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<Place> findByCategoryOrderByLikeCountDescWithCursor(
            @Param("categoryName") String categoryName,
            @Param("lastLikeCount") long lastLikeCount,
            @Param("lastCreatedAt") java.time.LocalDateTime lastCreatedAt,
            @Param("lastPlaceId") Long lastPlaceId,
            @Param("limit") int limit);

    /**
     * 장소 ID로 좋아요 수 조회
     * @param placeId 장소 ID
     * @return 좋아요 수
     */
    @Query(value = "SELECT COUNT(*) FROM place_likes " +
           "WHERE place_id = :placeId AND enabled = true", nativeQuery = true)
    long getLikeCountByPlaceId(@Param("placeId") Long placeId);

    /**
     * 키워드로 장소 검색 (이름, 주소, 요약 정보에서 검색)
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Query("SELECT DISTINCT p FROM Place p " +
           "WHERE p.enabled = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.summery) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.name ASC")
    Page<Place> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 키워드로 장소 검색 (카테고리 필터 포함)
     * @param keyword 검색 키워드
     * @param category 카테고리 enum (선택사항)
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Query("SELECT DISTINCT p FROM Place p " +
           "WHERE p.enabled = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.summery) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "ORDER BY p.name ASC")
    Page<Place> searchByKeywordAndCategory(@Param("keyword") String keyword,
                                           @Param("category") Category category,
                                           Pageable pageable);

}
