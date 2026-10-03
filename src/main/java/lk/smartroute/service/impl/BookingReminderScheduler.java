package lk.smartroute.service.impl;

import lk.smartroute.model.Booking;
import lk.smartroute.repository.BookingRepository;
import lk.smartroute.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookingReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(BookingReminderScheduler.class);

    private final BookingRepository bookings;
    private final NotificationService notifications;
    private final boolean thirtyMinuteReminderEnabled;

    public BookingReminderScheduler(
        BookingRepository bookings,
        NotificationService notifications,
        @Value("${app.notifications.reminders.30m-enabled:true}") boolean thirtyMinuteReminderEnabled
    ) {
        this.bookings = bookings;
        this.notifications = notifications;
        this.thirtyMinuteReminderEnabled = thirtyMinuteReminderEnabled;
    }

    @Scheduled(fixedDelayString = "${app.notifications.reminder-scheduler-ms:300000}")
    @Transactional
    public void sendTripReminders() {
        LocalDateTime now = LocalDateTime.now();
        sendWindow("24H", now.plusHours(24), 20);
        sendWindow("2H", now.plusHours(2), 10);
        if (thirtyMinuteReminderEnabled) {
            sendWindow("30M", now.plusMinutes(30), 5);
        }
    }

    private void sendWindow(String kind, LocalDateTime target, int toleranceMinutes) {
        LocalDateTime from = target.minusMinutes(toleranceMinutes);
        LocalDateTime to = target.plusMinutes(toleranceMinutes);
        for (Booking booking : bookings.findConfirmedReminderCandidates(target.toLocalDate(), from.toLocalTime(), to.toLocalTime(), kind)) {
            notifications.sendBookingReminder(booking);
            LocalDateTime sentAt = LocalDateTime.now();
            switch (kind) {
                case "24H" -> booking.setReminder24hSentAt(sentAt);
                case "2H" -> booking.setReminder2hSentAt(sentAt);
                case "30M" -> booking.setReminder30mSentAt(sentAt);
                default -> log.warn("Unknown reminder kind {}", kind);
            }
        }
    }
}
