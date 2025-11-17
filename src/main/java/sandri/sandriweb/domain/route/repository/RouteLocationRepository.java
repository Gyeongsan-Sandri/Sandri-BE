package sandri.sandriweb.domain.route.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.entity.RouteLocation;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RouteLocationRepository extends JpaRepository<RouteLocation, Long> {

    List<RouteLocation> findByRoute(Route route);

    /**
     * 특정 루트의 특정 dayNumber에 해당하는 장소 목록 조회
     * @param route 루트
     * @param dayNumber 날짜 번호 (1부터 시작)
     * @return 해당 dayNumber의 장소 목록 (displayOrder 순으로 정렬)
     */
    List<RouteLocation> findByRouteAndDayNumberOrderByDisplayOrderAsc(Route route, Integer dayNumber);
}

