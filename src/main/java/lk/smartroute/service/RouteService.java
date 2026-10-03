package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import org.springframework.data.domain.Pageable;

public interface RouteService {
    PageResponse<RouteResponse> list(Boolean active, Pageable pageable);

    RouteResponse get(Long id);

    RouteResponse create(RouteRequest request, String actorEmail);

    RouteResponse update(Long id, RouteRequest request, String actorEmail);

    void delete(Long id, String actorEmail);
}
