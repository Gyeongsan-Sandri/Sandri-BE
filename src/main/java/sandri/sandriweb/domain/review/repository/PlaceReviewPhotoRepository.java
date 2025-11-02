package sandri.sandriweb.domain.review.repository;

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
}

