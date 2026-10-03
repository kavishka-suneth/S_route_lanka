package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.Route;
import lk.smartroute.repository.RouteRepository;
import lk.smartroute.service.RouteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RouteServiceImpl implements RouteService {
    private final RouteRepository routes;

    public RouteServiceImpl(RouteRepository routes) {
        this.routes = routes;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RouteResponse> list(Boolean active, Pageable pageable) {
        Page<Route> page = active == null
            ? routes.findAll(pageable)
            : routes.findByActive(active, pageable);
        return PageResponse.from(page.map(DtoMapper::route));
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse get(Long id) {
        return DtoMapper.route(routeById(id));
    }

    @Override
    public RouteResponse create(RouteRequest request, String actorEmail) {
        if (routes.existsByOriginIgnoreCaseAndDestinationIgnoreCase(request.origin(), request.destination())) {
            throw new ConflictException("Route already exists");
        }
        Route route = new Route();
        apply(route, request);
        Route saved = routes.save(route);
        return DtoMapper.route(saved);
    }

    @Override
    public RouteResponse update(Long id, RouteRequest request, String actorEmail) {
        Route route = routeById(id);
        boolean sameRoute = route.getOrigin().equalsIgnoreCase(request.origin())
            && route.getDestination().equalsIgnoreCase(request.destination());
        if (!sameRoute && routes.existsByOriginIgnoreCaseAndDestinationIgnoreCase(request.origin(), request.destination())) {
            throw new ConflictException("Route already exists");
        }
        apply(route, request);
        return DtoMapper.route(route);
    }

    @Override
    public void delete(Long id, String actorEmail) {
        Route route = routeById(id);
        route.setActive(false);
    }

    private void apply(Route route, RouteRequest request) {
        route.setOrigin(request.origin().trim());
        route.setDestination(request.destination().trim());
        if (request.active() != null) {
            route.setActive(request.active());
        }
    }

    private Route routeById(Long id) {
        return routes.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
    }
}
