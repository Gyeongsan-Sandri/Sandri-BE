package sandri.sandriweb.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.user.entity.TravelStylePlace;
import sandri.sandriweb.domain.user.entity.User;

import java.util.List;

@Repository
public interface TravelStylePlaceRepository extends JpaRepository<TravelStylePlace, Long> {
    
    /**
     * TravelStyle로 매핑된 Place 목록 조회 (enabled된 것만)
     * @param travelStyle 여행 스타일
     * @return Place 목록
     */
    @Query("SELECT tsp.place FROM TravelStylePlace tsp " +
           "WHERE tsp.travelStyle = :travelStyle AND tsp.enabled = true " +
           "ORDER BY tsp.createdAt ASC")
    List<Place> findPlacesByTravelStyle(@Param("travelStyle") User.TravelStyle travelStyle);
    
    /**
     * TravelStyle과 PlaceId로 매핑 존재 여부 확인 (enabled된 것만)
     * @param travelStyle 여행 스타일
     * @param placeId 장소 ID
     * @return 매핑 엔티티 (존재하는 경우)
     */
    @Query("SELECT tsp FROM TravelStylePlace tsp " +
           "WHERE tsp.travelStyle = :travelStyle AND tsp.place.id = :placeId AND tsp.enabled = true")
    java.util.Optional<TravelStylePlace> findByTravelStyleAndPlaceId(
            @Param("travelStyle") User.TravelStyle travelStyle,
            @Param("placeId") Long placeId);
}

