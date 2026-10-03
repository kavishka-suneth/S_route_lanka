package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.BookingStatus;
import lk.smartroute.model.Role;
import lk.smartroute.security.SecurityUtils;
import lk.smartroute.service.BookingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/available-seats/{scheduleId}")
    public AvailableSeatsResponse availableSeats(@PathVariable Long scheduleId) {
        return bookings.availableSeats(scheduleId);
    }

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<BookingResponse> create(Authentication authentication, @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookings.create(authentication.getName(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public PageResponse<BookingResponse> list(
        @RequestParam(required = false) BookingStatus status,
        @RequestParam(required = false) String keyword,
        @PageableDefault(sort = "bookingDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return bookings.listAll(status, keyword, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public BookingResponse get(Authentication authentication, @PathVariable Long id) {
        return bookings.get(authentication.getName(), id, staffAccess(authentication));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PASSENGER')")
    public PageResponse<BookingResponse> history(Authentication authentication, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return bookings.history(authentication.getName(), pageable);
    }

    @PutMapping("/{id}/seats")
    @PreAuthorize("hasRole('PASSENGER')")
    public BookingResponse modifySeats(Authentication authentication, @PathVariable Long id, @Valid @RequestBody ModifyBookingRequest request) {
        return bookings.modifySeats(authentication.getName(), id, request);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PASSENGER','CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public BookingResponse cancel(Authentication authentication, @PathVariable Long id) {
        return bookings.cancel(authentication.getName(), id, staffAccess(authentication));
    }

    private boolean staffAccess(Authentication authentication) {
        return SecurityUtils.hasAnyRole(authentication, Role.CUSTOMER_SERVICE, Role.OPERATIONS_MANAGER, Role.SYSTEM_ADMIN);
    }
}
