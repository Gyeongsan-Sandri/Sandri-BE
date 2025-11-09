package sandri.sandriweb.domain.review.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.review.entity.PlaceReviewPhoto;

import java.util.List;

@Repository
public interface PlaceReviewPhotoRepository extends JpaRepository<PlaceReviewPhoto, Long> {
    
    List<PlaceReviewPhoto> findByPlaceReviewId(Long placeReviewId);
    
    @Query("SELECT p FROM PlaceReviewPhoto p WHERE p.place.id = :placeId ORDER BY p.createdAt DESC")
    List<PlaceReviewPhoto> findByPlaceId(@Param("placeId") Long placeId);

    // 커서 기반 페이징 - 최신순 (createdAt DESC, id DESC)
    @Query("SELECT p FROM PlaceReviewPhoto p " +
           "WHERE p.place.id = :placeId " +
           "AND p.enabled = true " +
           "AND (:lastPhotoId IS NULL OR " +
           "     p.createdAt < (SELECT p2.createdAt FROM PlaceReviewPhoto p2 WHERE p2.id = :lastPhotoId) OR " +
           "     (p.createdAt = (SELECT p2.createdAt FROM PlaceReviewPhoto p2 WHERE p2.id = :lastPhotoId) AND p.id < :lastPhotoId)) " +
           "ORDER BY p.createdAt DESC, p.id DESC")
    List<PlaceReviewPhoto> findByPlaceIdWithCursor(
            @Param("placeId") Long placeId,
            @Param("lastPhotoId") Long lastPhotoId,
            Pageable pageable);
    
    /**
     * 특정 장소의 활성화된 리뷰 사진 개수 조회
     * @param placeId 장소 ID
     * @return 리뷰 사진 개수
     */
    @Query("SELECT COUNT(p) FROM PlaceReviewPhoto p WHERE p.place.id = :placeId AND p.enabled = true")
    Long countByPlaceId(@Param("placeId") Long placeId);
}

