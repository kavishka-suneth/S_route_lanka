package lk.smartroute.event;

/**
 * Published after a booking payment has been persisted successfully.
 *
 * Notification delivery is handled after the booking transaction commits so
 * a slow SMTP server cannot keep the seat/payment request waiting.
 */
public record BookingPaymentCompletedEvent(Long bookingId, Long paymentId, Long walletTransactionId, boolean successful) {
}
