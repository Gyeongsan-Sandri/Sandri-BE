package sandri.sandriweb.domain.review.repository;

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
    
    @Query("SELECT AVG(r.rating) FROM PlaceReview r WHERE r.place.id = :placeId")
    Double findAverageRatingByPlaceId(@Param("placeId") Long placeId);

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
    
    /**
     * 특정 장소의 활성화된 리뷰 개수 조회
     * @param placeId 장소 ID
     * @return 리뷰 개수
     */
    @Query("SELECT COUNT(r) FROM PlaceReview r WHERE r.place.id = :placeId AND r.enabled = true")
    Long countByPlaceId(@Param("placeId") Long placeId);
    
    /**
     * 리뷰 ID로 리뷰 조회 (사진과 사용자 정보 포함)
     * @param reviewId 리뷰 ID
     * @return 리뷰 엔티티 (사진과 사용자 정보 포함)
     */
    @Query("SELECT DISTINCT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.photos " +
           "WHERE r.id = :reviewId")
    java.util.Optional<PlaceReview> findByIdWithPhotos(@Param("reviewId") Long reviewId);
    
    /**
     * 리뷰 ID로 리뷰 조회 (사용자 및 장소 정보 포함, 권한 확인 및 수정용)
     * @param reviewId 리뷰 ID
     * @return 리뷰 엔티티 (사용자 및 장소 정보 포함)
     */
    @Query("SELECT r FROM PlaceReview r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.place " +
           "WHERE r.id = :reviewId")
    java.util.Optional<PlaceReview> findByIdWithUserAndPlace(@Param("reviewId") Long reviewId);
    
    /**
     * 특정 사용자가 특정 장소에 작성한 활성화된 리뷰 존재 여부 확인
     * @param userId 사용자 ID
     * @param placeId 장소 ID
     * @return 리뷰 존재 여부
     */
    @Query("SELECT COUNT(r) > 0 FROM PlaceReview r WHERE r.user.id = :userId AND r.place.id = :placeId AND r.enabled = true")
    boolean existsByUserIdAndPlaceId(@Param("userId") Long userId, @Param("placeId") Long placeId);
}

