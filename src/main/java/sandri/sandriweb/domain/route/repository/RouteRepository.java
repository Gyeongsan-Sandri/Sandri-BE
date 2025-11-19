package sandri.sandriweb.domain.route.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    
    List<Route> findByCreator(User creator);
    
    List<Route> findByCreatorAndIsPublic(User creator, boolean isPublic);
    
    Optional<Route> findByShareCode(String shareCode);
    
    @Query("SELECT r FROM Route r JOIN r.participants p WHERE p.user = :user OR r.creator = :user")
    List<Route> findByParticipantOrCreator(@Param("user") User user);
    
    boolean existsByIdAndCreator(Long id, User creator);

    /**
     * 키워드로 루트 검색 (제목에서 검색, 공개된 루트만)
     * locations와 creator를 fetch join하여 N+1 문제 방지
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Query("SELECT DISTINCT r FROM Route r " +
           "LEFT JOIN FETCH r.locations " +
           "LEFT JOIN FETCH r.creator " +
           "WHERE r.isPublic = true " +
           "AND LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY r.createdAt DESC")
    Page<Route> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 사용자가 참여한 루트 중 오늘 날짜에 해당하는 루트 조회
     * @param userId 사용자 ID
     * @param today 오늘 날짜
     * @return 오늘 날짜에 해당하는 루트 목록
     */
    @Query("SELECT DISTINCT r FROM Route r " +
           "LEFT JOIN r.participants p " +
           "WHERE (r.creator.id = :userId OR p.user.id = :userId) " +
           "AND r.startDate <= :today " +
           "AND r.endDate >= :today")
    List<Route> findTodayRoutesByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);

    /**
     * 여러 ID로 루트 조회 (creator를 fetch join하여 N+1 문제 방지)
     * @param routeIds 루트 ID 목록
     * @return 루트 목록
     */
    @Query("SELECT DISTINCT r FROM Route r " +
           "LEFT JOIN FETCH r.creator " +
           "WHERE r.id IN :routeIds")
    List<Route> findAllByIdWithCreator(@Param("routeIds") List<Long> routeIds);
}

