package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.service.RouteService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService routes;

    public RouteController(RouteService routes) {
        this.routes = routes;
    }

    @GetMapping
    public PageResponse<RouteResponse> list(
        @RequestParam(required = false) Boolean active,
        @PageableDefault(sort = "origin") Pageable pageable
    ) {
        return routes.list(active, pageable);
    }

    @GetMapping("/{id}")
    public RouteResponse get(@PathVariable Long id) {
        return routes.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<RouteResponse> create(Authentication authentication, @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routes.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public RouteResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        return routes.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        routes.delete(id, authentication.getName());
    }
}
