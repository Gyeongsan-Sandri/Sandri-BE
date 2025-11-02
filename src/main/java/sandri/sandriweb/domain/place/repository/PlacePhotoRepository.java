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
    
}

