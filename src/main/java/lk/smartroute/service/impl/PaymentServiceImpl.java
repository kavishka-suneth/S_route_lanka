package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.PaymentRequest;
import lk.smartroute.dto.ApiDtos.PaymentResponse;
import lk.smartroute.dto.ApiDtos.ReceiptResponse;
import lk.smartroute.dto.ApiDtos.PageResponse;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.event.BookingPaymentCompletedEvent;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ForbiddenActionException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.*;
import lk.smartroute.repository.BookingRepository;
import lk.smartroute.repository.PaymentRepository;
import lk.smartroute.service.NotificationService;
import lk.smartroute.service.PaymentGateway;
import lk.smartroute.service.PaymentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final PaymentGateway paymentGateway;
    private final NotificationService notifications;
    private final lk.smartroute.service.WalletService walletService;
    private final ApplicationEventPublisher events;

    public PaymentServiceImpl(
        PaymentRepository payments,
        BookingRepository bookings,
        PaymentGateway paymentGateway,
        NotificationService notifications,
        lk.smartroute.service.WalletService walletService,
        ApplicationEventPublisher events
    ) {
        this.payments = payments;
        this.bookings = bookings;
        this.paymentGateway = paymentGateway;
        this.notifications = notifications;
        this.walletService = walletService;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listPayments(PaymentStatus status, String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.from(payments.searchPayments(status, normalizedKeyword, pageable).map(DtoMapper::payment));
    }

    @Override
    public PaymentResponse createPayment(String email, Long bookingId, PaymentRequest request) {
        Booking booking = bookingByIdForUpdate(bookingId);
        if (!booking.getPassenger().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenActionException("Booking does not belong to user");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Booking cannot be paid in its current status");
        }
        if (booking.getPaymentDueAt() != null && booking.getPaymentDueAt().isBefore(LocalDateTime.now())) { booking.setBookingStatus(BookingStatus.CANCELLED); booking.releaseSeats(); throw new ConflictException("Booking payment hold has expired"); }
        String transactionReference = request.transactionReference().trim();
        if (payments.existsByTransactionReference(transactionReference)) {
            throw new ConflictException("Payment transaction reference already exists");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTransactionReference(transactionReference);
        payment.setPaymentMethod(request.paymentMethod().trim());
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING);

        PaymentGateway.GatewayResult result = paymentGateway.process(booking, request);
        payment.setPaymentStatus(result.status());

        try {
            Payment saved = payments.saveAndFlush(payment);
            if (result.status() == PaymentStatus.SUCCESS) {
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                booking.setPaymentDueAt(null);
                events.publishEvent(new BookingPaymentCompletedEvent(booking.getId(), saved.getId(), null, true));
            } else if (result.status() == PaymentStatus.FAILED) {
                booking.setBookingStatus(BookingStatus.CANCELLED);
                booking.releaseSeats();
                events.publishEvent(new BookingPaymentCompletedEvent(booking.getId(), saved.getId(), null, false));
            }
            return DtoMapper.payment(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Payment transaction reference already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse verifyPayment(String email, Long paymentId, boolean staffAccess) {
        Payment payment = paymentById(paymentId);
        verifyOwnerOrStaff(payment, email, staffAccess);
        return DtoMapper.payment(payment);
    }

    @Override
    public PaymentResponse refund(Long paymentId, String actorEmail) {
        return refundPayment(paymentById(paymentId), null);
    }

    @Override
    public void refundSuccessfulPaymentForBooking(Long bookingId, String actorEmail, BigDecimal amount) {
        Payment payment = payments.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(bookingId, PaymentStatus.SUCCESS)
            .orElseThrow(() -> new ResourceNotFoundException("Successful payment not found for booking"));
        refundPayment(payment, amount);
    }

    private PaymentResponse refundPayment(Payment payment, BigDecimal requestedAmount) {
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded");
        }
        Booking booking = payment.getBooking();
        BigDecimal refundAmount = requestedAmount == null ? payment.getAmount() : requestedAmount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (refundAmount.signum() < 0 || refundAmount.compareTo(payment.getAmount()) > 0) throw new IllegalArgumentException("Invalid refund amount");
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Completed bookings cannot be refunded");
        }

        if ("SMARTROUTE_WALLET".equalsIgnoreCase(payment.getPaymentMethod())) {
            walletService.refundWalletPayment(booking.getId(), refundAmount);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            PaymentGateway.GatewayResult result = paymentGateway.refund(payment);
            if (result.status() != PaymentStatus.REFUNDED) {
                throw new IllegalStateException("Payment gateway could not process the refund");
            }
            payment.setPaymentStatus(result.status());
        }
        payment.setRefundedAmount(refundAmount);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.releaseSeats();
        notifications.sendBookingCancellation(booking);
        notifications.sendRefundConfirmation(payment);
        return DtoMapper.payment(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse receipt(String email, Long bookingId, boolean staffAccess) {
        Booking booking = bookingById(bookingId);
        if (!staffAccess && !booking.getPassenger().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenActionException("Booking does not belong to user");
        }
        Payment payment = payments.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(bookingId, PaymentStatus.SUCCESS)
            .or(() -> payments.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(bookingId, PaymentStatus.REFUNDED))
            .orElseThrow(() -> new ResourceNotFoundException("Receipt is available only after successful payment"));
        return new ReceiptResponse(
            "REC-" + payment.getId(),
            booking.getBookingReference(),
            booking.getPassenger().getUser().getFullName(),
            DtoMapper.routeLabel(booking.getSchedule()),
            DtoMapper.seats(booking),
            DtoMapper.passengerDetails(booking),
            payment.getAmount(),
            payment.getPaymentMethod(),
            payment.getTransactionReference(),
            payment.getPaymentDate()
        );
    }

    private Booking bookingById(Long id) {
        return bookings.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private Booking bookingByIdForUpdate(Long id) {
        return bookings.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private Payment paymentById(Long id) {
        return payments.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private void verifyOwnerOrStaff(Payment payment, String email, boolean staffAccess) {
        if (!staffAccess && !payment.getBooking().getPassenger().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenActionException("Payment does not belong to user");
        }
    }

}
