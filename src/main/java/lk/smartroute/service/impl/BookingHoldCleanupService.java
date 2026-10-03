package lk.smartroute.service.impl;
import lk.smartroute.model.BookingStatus;
import lk.smartroute.repository.BookingRepository;
import lk.smartroute.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Service
public class BookingHoldCleanupService {
 private final BookingRepository bookings; private final NotificationService notifications;
 public BookingHoldCleanupService(BookingRepository bookings, NotificationService notifications){this.bookings=bookings;this.notifications=notifications;}
 @Scheduled(fixedDelayString="${app.booking.hold-cleanup-ms:60000}")
 @Transactional
 public void expire(){
  LocalDateTime now=LocalDateTime.now();
  for(var b: bookings.findByBookingStatusAndPaymentDueAtBetweenAndPaymentDeadlineWarningSentAtIsNull(BookingStatus.PENDING_PAYMENT, now.plusMinutes(1), now.plusMinutes(10))) {
   notifications.sendBookingExpiryWarning(b); b.setPaymentDeadlineWarningSentAt(now);
  }
  for(var b: bookings.findByBookingStatusAndPaymentDueAtBefore(BookingStatus.PENDING_PAYMENT, now)) {
   b.setBookingStatus(BookingStatus.CANCELLED); b.releaseSeats();
   if(b.getExpiredNotificationSentAt()==null){notifications.sendBookingExpired(b); b.setExpiredNotificationSentAt(now);}
  }
 }
}
