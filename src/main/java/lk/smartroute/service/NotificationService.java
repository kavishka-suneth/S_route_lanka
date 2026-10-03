package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.NotificationResponse;
import lk.smartroute.dto.ApiDtos.ManualNotificationRequest;
import lk.smartroute.dto.ApiDtos.PageResponse;
import lk.smartroute.model.Booking;
import lk.smartroute.model.Payment;
import lk.smartroute.model.Schedule;
import lk.smartroute.model.SupportTicket;
import lk.smartroute.model.User;
import lk.smartroute.model.WalletTransaction;
import lk.smartroute.model.NotificationCategory;
import lk.smartroute.model.NotificationStatus;
import lk.smartroute.model.NotificationType;
import org.springframework.data.domain.Pageable;
import java.time.LocalTime;

public interface NotificationService {
    void sendBookingConfirmation(Booking booking);

    void sendPaymentConfirmation(Booking booking);

    default void sendPaymentReceipt(Payment payment) {
    }

    void sendScheduleDelay(Schedule schedule);

    default void sendScheduleDelay(Schedule schedule, LocalTime originalDepartureTime, String reason) {
        sendScheduleDelay(schedule);
    }

    void sendScheduleCancellation(Schedule schedule);

    default void sendScheduleUpdated(Schedule schedule, String summary) {
    }

    default void sendBusChanged(Schedule schedule, String previousBusRegistration) {
    }

    default void sendSeatChanged(Booking booking, String previousSeats, String newSeats) {
    }

    void sendBookingCancellation(Booking booking);

    default void sendRefundConfirmation(Payment payment) {
    }

    default void sendBookingReminder(Booking booking) {
    }

    default void sendPaymentFailed(Booking booking, Payment payment, String safeReason) {
    }

    default void sendBookingExpiryWarning(Booking booking) {
    }

    default void sendBookingExpired(Booking booking) {
    }

    void sendSupportTicketResponse(SupportTicket ticket);

    default void sendSupportTicketCreated(SupportTicket ticket) {
    }

    default void sendSupportTicketAssigned(SupportTicket ticket) {
    }

    default void sendSupportTicketStatusChanged(SupportTicket ticket) {
    }

    default void sendSecurityNotification(User user, NotificationType type, String title, String message, String relatedReference) {
    }

    default void sendWalletNotification(WalletTransaction transaction) {
    }

    default PageResponse<NotificationResponse> sendManual(String actorEmail, ManualNotificationRequest request) {
        throw new UnsupportedOperationException("Manual notifications are not supported");
    }

    PageResponse<NotificationResponse> listForUser(String email, NotificationStatus status, NotificationCategory category, Pageable pageable);

    default PageResponse<NotificationResponse> listForUser(String email, Pageable pageable) {
        return listForUser(email, null, null, pageable);
    }

    NotificationResponse markRead(String email, Long notificationId);

    default void markAllRead(String email) {
    }

    default void archive(String email, Long notificationId) {
    }

    PageResponse<NotificationResponse> listAll(NotificationStatus status, Pageable pageable);

    long unreadCount(String email);
}
