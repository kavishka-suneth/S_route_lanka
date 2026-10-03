package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.AvailabilityStatus;
import lk.smartroute.service.DriverService;
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
@RequestMapping("/api/drivers")
@PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
public class DriverController {
    private final DriverService drivers;

    public DriverController(DriverService drivers) {
        this.drivers = drivers;
    }

    @GetMapping
    public PageResponse<DriverResponse> list(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) AvailabilityStatus availabilityStatus,
        @PageableDefault(sort = "firstName") Pageable pageable
    ) {
        return drivers.list(active, availabilityStatus, pageable);
    }

    @GetMapping("/available")
    public List<DriverResponse> available(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departureTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime arrivalTime,
        @RequestParam(required = false) Long excludeScheduleId
    ) {
        return drivers.available(travelDate, departureTime, arrivalTime, excludeScheduleId);
    }

    @GetMapping("/{id}")
    public DriverResponse get(@PathVariable Long id) {
        return drivers.get(id);
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(Authentication authentication, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(drivers.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public DriverResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        return drivers.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        drivers.delete(id, authentication.getName());
    }
}
