package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.SupportTicketStatus;
import lk.smartroute.service.SupportTicketService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support-tickets")
public class SupportTicketController {
    private final SupportTicketService supportTickets;

    public SupportTicketController(SupportTicketService supportTickets) {
        this.supportTickets = supportTickets;
    }

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(Authentication authentication, @Valid @RequestBody SupportTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTickets.create(authentication.getName(), request));
    }

    @GetMapping("/my")
    public PageResponse<SupportTicketResponse> mine(Authentication authentication, @PageableDefault(sort = "createdDate") Pageable pageable) {
        return supportTickets.listMine(authentication.getName(), pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public PageResponse<SupportTicketResponse> list(
        @RequestParam(required = false) SupportTicketStatus status,
        @RequestParam(required = false) Boolean unassigned,
        @RequestParam(required = false) String keyword,
        @PageableDefault(sort = "createdDate") Pageable pageable
    ) {
        return supportTickets.listAll(status, unassigned, keyword, pageable);
    }

    @PatchMapping("/{id}/response")
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public SupportTicketResponse respond(Authentication authentication, @PathVariable Long id, @Valid @RequestBody SupportTicketResponseRequest request) {
        return supportTickets.respond(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public SupportTicketResponse updateStatus(@PathVariable Long id, @Valid @RequestBody SupportTicketStatusUpdateRequest request) {
        return supportTickets.updateStatus(id, request);
    }
}
