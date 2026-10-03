package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.service.ScheduleService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {
    private final ScheduleService schedules;

    public ScheduleController(ScheduleService schedules) {
        this.schedules = schedules;
    }

    @GetMapping("/search")
    public PageResponse<ScheduleResponse> search(
        @RequestParam String origin,
        @RequestParam String destination,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @PageableDefault(sort = "departureTime") Pageable pageable
    ) {
        return schedules.search(origin, destination, date, pageable);
    }

    @GetMapping("/upcoming")
    public PageResponse<ScheduleResponse> upcoming(@PageableDefault(sort = "travelDate") Pageable pageable) {
        return schedules.upcoming(pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','CUSTOMER_SERVICE','SYSTEM_ADMIN')")
    public PageResponse<ScheduleResponse> list(@PageableDefault(sort = "departureTime") Pageable pageable) {
        return schedules.list(pageable);
    }

    @GetMapping("/{id}")
    public ScheduleResponse get(@PathVariable Long id) {
        return schedules.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public ResponseEntity<ScheduleResponse> create(Authentication authentication, @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedules.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public ScheduleResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return schedules.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public ScheduleResponse updateStatus(
        Authentication authentication,
        @PathVariable Long id,
        @Valid @RequestBody ScheduleStatusUpdateRequest request
    ) {
        return schedules.updateStatus(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRANSPORT_COORDINATOR','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        schedules.delete(id, authentication.getName());
    }
}
