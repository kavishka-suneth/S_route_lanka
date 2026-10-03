package lk.smartroute.service.impl;

import lk.smartroute.event.BookingPaymentCompletedEvent;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.Booking;
import lk.smartroute.model.BookingStatus;
import lk.smartroute.model.Payment;
import lk.smartroute.model.PaymentStatus;
import lk.smartroute.repository.BookingRepository;
import lk.smartroute.repository.PaymentRepository;
import lk.smartroute.repository.WalletTransactionRepository;
import lk.smartroute.service.EmailService;
import lk.smartroute.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends non-critical booking side effects after payment has committed. */
@Component
public class BookingPaymentEventListener {
    private static final Logger log = LoggerFactory.getLogger(BookingPaymentEventListener.class);

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final WalletTransactionRepository walletTransactions;
    private final NotificationService notifications;
    private final TicketQrService ticketQr;
    private final EmailService emailService;

    public BookingPaymentEventListener(
        BookingRepository bookings,
        PaymentRepository payments,
        WalletTransactionRepository walletTransactions,
        NotificationService notifications,
        TicketQrService ticketQr,
        EmailService emailService
    ) {
        this.bookings = bookings;
        this.payments = payments;
        this.walletTransactions = walletTransactions;
        this.notifications = notifications;
        this.ticketQr = ticketQr;
        this.emailService = emailService;
    }

    @Async("bookingNotificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(BookingPaymentCompletedEvent event) {
        try {
            Booking booking = bookings.findById(event.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
            Payment payment = payments.findById(event.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            if (event.successful()
                && (payment.getPaymentStatus() != PaymentStatus.SUCCESS
                    || booking.getBookingStatus() != BookingStatus.CONFIRMED)) {
                return;
            }

            if (event.walletTransactionId() != null) {
                walletTransactions.findById(event.walletTransactionId())
                    .ifPresent(notifications::sendWalletNotification);
            }
            if (event.successful()) {
                notifications.sendBookingConfirmation(booking);
                notifications.sendPaymentConfirmation(booking);
                notifications.sendPaymentReceipt(payment);
                sendTicketEmail(booking);
            } else {
                notifications.sendPaymentFailed(booking, payment, "The payment was declined");
                notifications.sendBookingCancellation(booking);
            }
        } catch (Exception ex) {
            // Payment is already committed; delivery failures must not undo it.
            log.warn("Post-payment notification delivery failed for booking {}", event.bookingId(), ex);
        }
    }

    private void sendTicketEmail(Booking booking) {
        try {
            String email = booking.getPassenger().getUser().getEmail();
            var qr = ticketQr.generate(email, booking.getId(), false);
            emailService.sendTicket(email, booking, qr.qrCodeDataUri());
        } catch (Exception ex) {
            log.warn("Ticket email could not be sent for booking {}", booking.getBookingReference(), ex);
        }
    }
}
