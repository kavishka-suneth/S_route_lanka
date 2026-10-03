package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.PaymentRequest;
import lk.smartroute.dto.ApiDtos.PaymentResponse;
import lk.smartroute.dto.ApiDtos.ReceiptResponse;
import lk.smartroute.dto.ApiDtos.PageResponse;
import lk.smartroute.model.PaymentStatus;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface PaymentService {
    PageResponse<PaymentResponse> listPayments(PaymentStatus status, String keyword, Pageable pageable);

    PaymentResponse createPayment(String email, Long bookingId, PaymentRequest request);

    PaymentResponse verifyPayment(String email, Long paymentId, boolean staffAccess);

    PaymentResponse refund(Long paymentId, String actorEmail);

    /** Refund the latest successful payment for an eligible booking. */
    default void refundSuccessfulPaymentForBooking(Long bookingId, String actorEmail) { refundSuccessfulPaymentForBooking(bookingId, actorEmail, null); }
    void refundSuccessfulPaymentForBooking(Long bookingId, String actorEmail, BigDecimal amount);

    ReceiptResponse receipt(String email, Long bookingId, boolean staffAccess);
}
