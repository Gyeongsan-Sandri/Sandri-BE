package sandri.sandriweb.domain.review.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.review.entity.PlaceReview;

import java.util.List;

@Repository
public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {
    
    List<PlaceReview> findByPlaceIdOrderByCreatedAtDesc(Long placeId);
    
    // 최신순
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "ORDER BY r.createdAt DESC")
    List<PlaceReview> findReviewsByPlaceIdOrderByLatest(@Param("placeId") Long placeId);
    
    // 평점 높은 순
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "ORDER BY r.rating DESC, r.createdAt DESC")
    List<PlaceReview> findReviewsByPlaceIdOrderByRatingDesc(@Param("placeId") Long placeId);
    
    // 평점 낮은 순
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "ORDER BY r.rating ASC, r.createdAt DESC")
    List<PlaceReview> findReviewsByPlaceIdOrderByRatingAsc(@Param("placeId") Long placeId);
    
    @Query("SELECT AVG(r.rating) FROM PlaceReview r WHERE r.place.id = :placeId")
    Double findAverageRatingByPlaceId(@Param("placeId") Long placeId);
    
    /**
     * 여러 장소의 평균 평점을 한 번에 조회 (배치 조회)
     * @param placeIds 장소 ID 목록
     * @return [placeId, averageRating] 형태의 Object[] 리스트
     */
    @Query("SELECT r.place.id, AVG(r.rating) FROM PlaceReview r WHERE r.place.id IN :placeIds GROUP BY r.place.id")
    List<Object[]> findAverageRatingsByPlaceIds(@Param("placeIds") List<Long> placeIds);
    
    // 페이징 지원 - 최신순
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "ORDER BY r.createdAt DESC")
    Page<PlaceReview> findReviewsByPlaceIdOrderByLatestWithPaging(@Param("placeId") Long placeId, Pageable pageable);
    
    // 페이징 지원 - 평점 높은 순
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "ORDER BY r.rating DESC, r.createdAt DESC")
    Page<PlaceReview> findReviewsByPlaceIdOrderByRatingDescWithPaging(@Param("placeId") Long placeId, Pageable pageable);
    
    // 페이징 지원 - 평점 낮은 순
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "ORDER BY r.rating ASC, r.createdAt DESC")
    Page<PlaceReview> findReviewsByPlaceIdOrderByRatingAscWithPaging(@Param("placeId") Long placeId, Pageable pageable);

    // 유저가 작성한 리뷰 목록 조회 (페이징 지원) - 최신순
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "LEFT JOIN FETCH r.place " +
           "WHERE r.user.id = :userId " +
           "ORDER BY r.createdAt DESC")
    Page<PlaceReview> findReviewsByUserIdOrderByLatestWithPaging(@Param("userId") Long userId, Pageable pageable);
}

