package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.AvailabilityStatus;
import lk.smartroute.service.BusService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/buses")
@PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
public class BusController {
    private final BusService buses;

    public BusController(BusService buses) {
        this.buses = buses;
    }

    @GetMapping
    public PageResponse<BusResponse> list(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) AvailabilityStatus availabilityStatus,
        @PageableDefault(sort = "registrationNo") Pageable pageable
    ) {
        return buses.list(active, availabilityStatus, pageable);
    }

    @GetMapping("/available")
    public List<BusResponse> available(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departureTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime arrivalTime,
        @RequestParam(required = false) Long excludeScheduleId
    ) {
        return buses.available(travelDate, departureTime, arrivalTime, excludeScheduleId);
    }

    @GetMapping("/{id}")
    public BusResponse get(@PathVariable Long id) {
        return buses.get(id);
    }

    @PostMapping
    public ResponseEntity<BusResponse> create(Authentication authentication, @Valid @RequestBody BusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(buses.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public BusResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody BusRequest request) {
        return buses.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        buses.delete(id, authentication.getName());
    }
}
