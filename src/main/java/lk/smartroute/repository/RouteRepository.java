package lk.smartroute.repository;

import lk.smartroute.model.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Page<Route> findByActive(boolean active, Pageable pageable);

    List<Route> findByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);

    @Query("""
        select r
        from Route r
        where lower(r.origin) like lower(concat('%', :keyword, '%'))
           or lower(r.destination) like lower(concat('%', :keyword, '%'))
        """)
    List<Route> searchByOriginOrDestination(@Param("keyword") String keyword);

    boolean existsByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);

    @Query("""
        select r.id as routeId, r.origin as origin, r.destination as destination, count(s.id) as scheduleCount
        from Route r left join Schedule s on s.route = r
        group by r.id, r.origin, r.destination
        """)
    List<RouteScheduleCount> countSchedulesByRoute();

    interface RouteScheduleCount {
        Long getRouteId();
        String getOrigin();
        String getDestination();
        long getScheduleCount();
    }
}
