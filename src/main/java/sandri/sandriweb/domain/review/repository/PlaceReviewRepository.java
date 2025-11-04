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

    // 커서 기반 페이징 - 내가 작성한 리뷰 목록 (최신순)
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "LEFT JOIN FETCH r.place " +
           "WHERE r.user.id = :userId " +
           "AND r.enabled = true " +
           "AND (:lastReviewId IS NULL OR " +
           "     r.createdAt < (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) OR " +
           "     (r.createdAt = (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) AND r.id < :lastReviewId)) " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<PlaceReview> findReviewsByUserIdOrderByLatestWithCursor(
            @Param("userId") Long userId,
            @Param("lastReviewId") Long lastReviewId,
            Pageable pageable);

    // 커서 기반 페이징 - 최신순 (createdAt DESC, id DESC)
    // JPQL은 LIMIT을 직접 지원하지 않지만, Spring Data JPA의 @Query에 Pageable을 사용하거나
    // 네이티브 쿼리를 사용할 수 있습니다. 여기서는 Pageable을 사용하여 LIMIT 적용
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "AND r.enabled = true " +
           "AND (:lastReviewId IS NULL OR " +
           "     r.createdAt < (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) OR " +
           "     (r.createdAt = (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) AND r.id < :lastReviewId)) " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<PlaceReview> findReviewsByPlaceIdOrderByLatestWithCursor(
            @Param("placeId") Long placeId,
            @Param("lastReviewId") Long lastReviewId,
            Pageable pageable);

    // 커서 기반 페이징 - 평점 높은 순 (rating DESC, createdAt DESC, id DESC)
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "AND r.enabled = true " +
           "AND (:lastReviewId IS NULL OR " +
           "     r.rating < (SELECT r2.rating FROM PlaceReview r2 WHERE r2.id = :lastReviewId) OR " +
           "     (r.rating = (SELECT r2.rating FROM PlaceReview r2 WHERE r2.id = :lastReviewId) AND " +
           "      (r.createdAt < (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) OR " +
           "       (r.createdAt = (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) AND r.id < :lastReviewId)))) " +
           "ORDER BY r.rating DESC, r.createdAt DESC, r.id DESC")
    List<PlaceReview> findReviewsByPlaceIdOrderByRatingDescWithCursor(
            @Param("placeId") Long placeId,
            @Param("lastReviewId") Long lastReviewId,
            Pageable pageable);

    // 커서 기반 페이징 - 평점 낮은 순 (rating ASC, createdAt DESC, id DESC)
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.place.id = :placeId " +
           "AND r.enabled = true " +
           "AND (:lastReviewId IS NULL OR " +
           "     r.rating > (SELECT r2.rating FROM PlaceReview r2 WHERE r2.id = :lastReviewId) OR " +
           "     (r.rating = (SELECT r2.rating FROM PlaceReview r2 WHERE r2.id = :lastReviewId) AND " +
           "      (r.createdAt < (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) OR " +
           "       (r.createdAt = (SELECT r2.createdAt FROM PlaceReview r2 WHERE r2.id = :lastReviewId) AND r.id < :lastReviewId)))) " +
           "ORDER BY r.rating ASC, r.createdAt DESC, r.id DESC")
    List<PlaceReview> findReviewsByPlaceIdOrderByRatingAscWithCursor(
            @Param("placeId") Long placeId,
            @Param("lastReviewId") Long lastReviewId,
            Pageable pageable);
}

