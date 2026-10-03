package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.PageResponse;
import lk.smartroute.dto.ApiDtos.PaymentRequest;
import lk.smartroute.dto.ApiDtos.PaymentResponse;
import lk.smartroute.dto.ApiDtos.ReceiptResponse;
import lk.smartroute.model.PaymentStatus;
import lk.smartroute.model.Role;
import lk.smartroute.security.SecurityUtils;
import lk.smartroute.service.PaymentService;
import lk.smartroute.service.impl.TicketQrService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService payments;
    private final TicketQrService ticketQr;

    public PaymentController(PaymentService payments, TicketQrService ticketQr) {
        this.payments = payments; this.ticketQr = ticketQr;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','SYSTEM_ADMIN')")
    public PageResponse<PaymentResponse> list(
        @RequestParam(required = false) PaymentStatus status,
        @RequestParam(required = false) String keyword,
        @PageableDefault(sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return payments.listPayments(status, keyword, pageable);
    }

    @PostMapping("/bookings/{bookingId}")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<PaymentResponse> create(
        Authentication authentication,
        @PathVariable Long bookingId,
        @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payments.createPayment(authentication.getName(), bookingId, request));
    }

    @GetMapping("/{paymentId}/verify")
    @PreAuthorize("hasAnyRole('PASSENGER','FINANCE_OFFICER','SYSTEM_ADMIN')")
    public PaymentResponse verify(Authentication authentication, @PathVariable Long paymentId) {
        boolean staff = SecurityUtils.hasAnyRole(authentication, Role.FINANCE_OFFICER, Role.SYSTEM_ADMIN);
        return payments.verifyPayment(authentication.getName(), paymentId, staff);
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','SYSTEM_ADMIN')")
    public PaymentResponse refund(Authentication authentication, @PathVariable Long paymentId) {
        return payments.refund(paymentId, authentication.getName());
    }

    @GetMapping("/bookings/{bookingId}/receipt")
    @PreAuthorize("hasAnyRole('PASSENGER','CUSTOMER_SERVICE','FINANCE_OFFICER','SYSTEM_ADMIN')")
    public ReceiptResponse receipt(Authentication authentication, @PathVariable Long bookingId) {
        boolean staff = SecurityUtils.hasAnyRole(authentication, Role.CUSTOMER_SERVICE, Role.FINANCE_OFFICER, Role.SYSTEM_ADMIN);
        return payments.receipt(authentication.getName(), bookingId, staff);
    }

    @GetMapping("/bookings/{bookingId}/ticket-qr")
    @PreAuthorize("hasAnyRole('PASSENGER','CUSTOMER_SERVICE','FINANCE_OFFICER','SYSTEM_ADMIN')")
    public lk.smartroute.dto.ApiDtos.QrTicketResponse ticketQr(Authentication authentication,@PathVariable Long bookingId){ boolean staff=SecurityUtils.hasAnyRole(authentication,Role.CUSTOMER_SERVICE,Role.FINANCE_OFFICER,Role.SYSTEM_ADMIN); return ticketQr.generate(authentication.getName(),bookingId,staff); }
}
