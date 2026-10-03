package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.ManualNotificationRequest;
import lk.smartroute.dto.ApiDtos.NotificationResponse;
import lk.smartroute.dto.ApiDtos.PageResponse;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ForbiddenActionException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.*;
import lk.smartroute.repository.*;
import lk.smartroute.service.EmailService;
import lk.smartroute.service.NotificationService;
import lk.smartroute.service.NotificationStreamService;
import lk.smartroute.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class LoggingNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);
    private static final List<BookingStatus> AFFECTED_BOOKINGS = List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);

    private final NotificationRepository notifications;
    private final BookingRepository bookings;
    private final NotificationPreferenceRepository preferences;
    private final EmailService emailService;
    private final SmsService smsService;
    private final UserRepository users;
    private final PassengerRepository passengers;
    private final ScheduleRepository schedules;
    private final NotificationStreamService streams;

    public LoggingNotificationService(
        NotificationRepository notifications,
        BookingRepository bookings,
        NotificationPreferenceRepository preferences,
        EmailService emailService,
        SmsService smsService,
        UserRepository users,
        PassengerRepository passengers,
        ScheduleRepository schedules,
        NotificationStreamService streams
    ) {
        this.notifications = notifications;
        this.bookings = bookings;
        this.preferences = preferences;
        this.emailService = emailService;
        this.smsService = smsService;
        this.users = users;
        this.passengers = passengers;
        this.schedules = schedules;
        this.streams = streams;
    }

    @Override
    public void sendBookingConfirmation(Booking booking) {
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.BOOKING_CONFIRMED,
            "Booking confirmed",
            "Booking " + booking.getBookingReference() + " is confirmed for " + routeLabel(booking.getSchedule()) + ".",
            booking.getBookingReference(),
            "BOOKING",
            booking.getId(),
            "/my-bookings.html",
            NotificationPriority.HIGH,
            bookingEmail(booking, "Booking confirmed", null)
        );
    }

    @Override
    public void sendPaymentConfirmation(Booking booking) {
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.PAYMENT_SUCCESS,
            "Payment received",
            "Payment for booking " + booking.getBookingReference() + " was received.",
            "PAYMENT-SUCCESS:" + booking.getBookingReference(),
            "BOOKING",
            booking.getId(),
            "/my-bookings.html",
            NotificationPriority.HIGH,
            bookingEmail(booking, "Payment received", null)
        );
    }

    @Override
    public void sendPaymentReceipt(Payment payment) {
        Booking booking = payment.getBooking();
        String reference = "PAYMENT:" + payment.getId();
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.PAYMENT_RECEIPT,
            "Payment receipt",
            "Booking " + booking.getBookingReference() + " payment " + displayMoney(payment.getAmount()) + " by " + payment.getPaymentMethod()
                + " is " + payment.getPaymentStatus() + ". Reference: " + safe(payment.getTransactionReference()) + ".",
            reference,
            "PAYMENT",
            payment.getId(),
            "/my-bookings.html",
            NotificationPriority.NORMAL,
            paymentReceiptEmail(payment)
        );
    }

    @Override
    public void sendPaymentFailed(Booking booking, Payment payment, String safeReason) {
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.PAYMENT_FAILED,
            "Payment failed",
            "Payment for booking " + booking.getBookingReference() + " could not be completed. "
                + safeReason(safeReason) + " Please try again before the payment deadline or create a new booking.",
            "PAYMENT-FAILED:" + (payment.getId() == null ? payment.getTransactionReference() : payment.getId()),
            "PAYMENT",
            payment.getId(),
            "/checkout.html",
            NotificationPriority.HIGH,
            simpleEmail("Payment failed", "Payment for booking " + booking.getBookingReference() + " could not be completed. Please try again.")
        );
    }

    @Override
    public void sendScheduleDelay(Schedule schedule) {
        sendScheduleDelay(schedule, schedule.getOriginalDepartureTime(), schedule.getDelayReason());
    }

    @Override
    public void sendScheduleDelay(Schedule schedule, LocalTime originalDepartureTime, String reason) {
        LocalTime original = originalDepartureTime == null ? schedule.getDepartureTime() : originalDepartureTime;
        long minutes = Math.max(0, Duration.between(original, schedule.getDepartureTime()).toMinutes());
        String delay = minutes > 0 ? minutes + " minutes" : "the updated departure time";
        String message = "Your " + routeLabel(schedule) + " trip has been delayed by " + delay
            + ". New departure time: " + schedule.getDepartureTime()
            + ". Reason: " + safeDefault(reason, "Operational delay") + "."
            + " Bus: " + busLabel(schedule) + ".";
        forAffectedBookings(schedule, booking -> notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.SCHEDULE_DELAYED,
            "Schedule delayed",
            message + " Booking: " + booking.getBookingReference() + ".",
            "SCHEDULE-DELAY:" + schedule.getId() + ":" + booking.getId() + ":" + schedule.getDepartureTime(),
            "SCHEDULE",
            schedule.getId(),
            "/my-bookings.html",
            NotificationPriority.CRITICAL,
            scheduleEmail(booking, "Schedule delayed", message)
        ));
        schedule.setLastDelayNotifiedAt(LocalDateTime.now());
    }

    @Override
    public void sendScheduleCancellation(Schedule schedule) {
        forAffectedBookings(schedule, booking -> notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.SCHEDULE_CANCELLED,
            "Schedule cancelled",
            "Your " + routeLabel(schedule) + " trip was cancelled. Booking: " + booking.getBookingReference() + ".",
            "SCHEDULE-CANCELLED:" + schedule.getId() + ":" + booking.getId(),
            "SCHEDULE",
            schedule.getId(),
            "/my-bookings.html",
            NotificationPriority.CRITICAL,
            scheduleEmail(booking, "Schedule cancelled", "Refunds are processed according to the payment method used. Contact support if you need help.")
        ));
    }

    @Override
    public void sendScheduleUpdated(Schedule schedule, String summary) {
        forAffectedBookings(schedule, booking -> notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.SCHEDULE_UPDATED,
            "Schedule updated",
            "Your " + routeLabel(schedule) + " trip was updated. " + safeDefault(summary, "Please review your booking details."),
            "SCHEDULE-UPDATED:" + schedule.getId() + ":" + booking.getId() + ":" + hash(summary),
            "SCHEDULE",
            schedule.getId(),
            "/my-bookings.html",
            NotificationPriority.HIGH,
            scheduleEmail(booking, "Schedule updated", safeDefault(summary, "Please review your booking details."))
        ));
        schedule.setLastUpdateNotifiedAt(LocalDateTime.now());
    }

    @Override
    public void sendBusChanged(Schedule schedule, String previousBusRegistration) {
        String message = "Bus for " + routeLabel(schedule) + " on " + schedule.getTravelDate() + " " + schedule.getDepartureTime()
            + " changed from " + safeDefault(previousBusRegistration, "the previous bus") + " to " + busLabel(schedule)
            + ". Seat numbers are preserved unless notified separately.";
        forAffectedBookings(schedule, booking -> notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.BUS_CHANGED,
            "Bus changed",
            message + " Booking: " + booking.getBookingReference() + ".",
            "BUS-CHANGED:" + schedule.getId() + ":" + booking.getId() + ":" + safe(previousBusRegistration) + ":" + busLabel(schedule),
            "SCHEDULE",
            schedule.getId(),
            "/my-bookings.html",
            NotificationPriority.HIGH,
            scheduleEmail(booking, "Bus changed", message)
        ));
    }

    @Override
    public void sendSeatChanged(Booking booking, String previousSeats, String newSeats) {
        if (Objects.equals(previousSeats, newSeats)) {
            return;
        }
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.SEAT_CHANGED,
            "Seat changed",
            "Your seat for Booking " + booking.getBookingReference() + " has been changed from Seat " + previousSeats + " to Seat " + newSeats + ".",
            "SEAT-CHANGED:" + booking.getId() + ":" + previousSeats + ":" + newSeats,
            "BOOKING",
            booking.getId(),
            "/my-bookings.html",
            NotificationPriority.HIGH,
            bookingEmail(booking, "Seat changed", "Your new seat assignment is " + newSeats + ".")
        );
    }

    @Override
    public void sendBookingCancellation(Booking booking) {
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.BOOKING_CANCELLED,
            "Booking cancelled",
            "Booking " + booking.getBookingReference() + " was cancelled.",
            "BOOKING-CANCELLED:" + booking.getBookingReference(),
            "BOOKING",
            booking.getId(),
            "/my-bookings.html",
            NotificationPriority.HIGH,
            bookingEmail(booking, "Booking cancelled", "Reserved seats were released. Any eligible refund will be processed separately.")
        );
    }

    @Override
    public void sendRefundConfirmation(Payment payment) {
        Booking booking = payment.getBooking();
        boolean wallet = "SMARTROUTE_WALLET".equalsIgnoreCase(payment.getPaymentMethod());
        String methodText = wallet ? "SmartRoute Wallet" : payment.getPaymentMethod();
        String message = "Your refund of " + displayMoney(payment.getRefundedAmount()) + " for booking " + booking.getBookingReference()
            + " has been " + (wallet ? "credited to your SmartRoute Wallet" : "processed") + ". Method: " + methodText
            + ". Reference: " + safe(payment.getTransactionReference()) + ". Status: " + payment.getPaymentStatus() + ".";
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.REFUND_COMPLETED,
            "Refund processed",
            message,
            "REFUND:" + payment.getId() + ":" + payment.getPaymentStatus(),
            "PAYMENT",
            payment.getId(),
            wallet ? "/wallet.html" : "/my-bookings.html",
            NotificationPriority.HIGH,
            refundEmail(payment)
        );
    }

    @Override
    public void sendBookingReminder(Booking booking) {
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.BOOKING_REMINDER,
            "Upcoming trip reminder",
            "Your " + routeLabel(booking.getSchedule()) + " trip for booking " + booking.getBookingReference()
                + " departs at " + booking.getSchedule().getDepartureTime() + ". Please arrive early.",
            "REMINDER:" + booking.getId() + ":" + booking.getSchedule().getDepartureDateTime(),
            "BOOKING",
            booking.getId(),
            "/my-bookings.html",
            NotificationPriority.HIGH,
            bookingEmail(booking, "Upcoming trip reminder", "Your bus departs soon. Please arrive at the boarding point early.")
        );
    }

    @Override
    public void sendBookingExpiryWarning(Booking booking) {
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.BOOKING_EXPIRY_WARNING,
            "Booking payment deadline",
            "Booking " + booking.getBookingReference() + " will expire in about 10 minutes if payment is not completed.",
            "EXPIRY-WARNING:" + booking.getId(),
            "BOOKING",
            booking.getId(),
            "/checkout.html",
            NotificationPriority.HIGH,
            bookingEmail(booking, "Booking payment deadline", "Complete payment soon to keep your selected seats.")
        );
    }

    @Override
    public void sendBookingExpired(Booking booking) {
        notifyUser(
            booking.getPassenger().getUser(),
            NotificationType.BOOKING_EXPIRED,
            "Booking expired",
            "Booking " + booking.getBookingReference() + " expired because payment was not completed. Seats have been released.",
            "EXPIRED:" + booking.getId(),
            "BOOKING",
            booking.getId(),
            "/search-results.html",
            NotificationPriority.HIGH,
            bookingEmail(booking, "Booking expired", "Your selected seats have been released.")
        );
    }

    @Override
    public void sendSupportTicketResponse(SupportTicket ticket) {
        notifyUser(
            ticket.getUser(),
            NotificationType.SUPPORT_REPLY,
            "Support replied",
            "Your support ticket \"" + ticket.getSubject() + "\" has a response.",
            "SUPPORT-REPLY:" + ticket.getId() + ":" + ticket.getUpdatedAt(),
            "SUPPORT_TICKET",
            ticket.getId(),
            "/support.html",
            NotificationPriority.NORMAL,
            simpleEmail("Support reply", "Your support ticket \"" + ticket.getSubject() + "\" has a response.")
        );
    }

    @Override
    public void sendSupportTicketCreated(SupportTicket ticket) {
        notifyUser(ticket.getUser(), NotificationType.SUPPORT_TICKET_CREATED, "Support ticket created",
            "Support ticket \"" + ticket.getSubject() + "\" was created.", "SUPPORT-CREATED:" + ticket.getId(),
            "SUPPORT_TICKET", ticket.getId(), "/support.html", NotificationPriority.NORMAL,
            simpleEmail("Support ticket created", "Support ticket \"" + ticket.getSubject() + "\" was created."));
    }

    @Override
    public void sendSupportTicketAssigned(SupportTicket ticket) {
        notifyUser(ticket.getUser(), NotificationType.SUPPORT_ASSIGNED, "Support ticket assigned",
            "Support ticket \"" + ticket.getSubject() + "\" was assigned to staff.", "SUPPORT-ASSIGNED:" + ticket.getId() + ":" + staffId(ticket),
            "SUPPORT_TICKET", ticket.getId(), "/support.html", NotificationPriority.NORMAL,
            simpleEmail("Support ticket assigned", "Support ticket \"" + ticket.getSubject() + "\" was assigned to staff."));
    }

    @Override
    public void sendSupportTicketStatusChanged(SupportTicket ticket) {
        NotificationType type = switch (ticket.getStatus()) {
            case RESOLVED -> NotificationType.SUPPORT_RESOLVED;
            case CLOSED -> NotificationType.SUPPORT_CLOSED;
            default -> NotificationType.SUPPORT_STATUS_CHANGED;
        };
        notifyUser(ticket.getUser(), type, "Support ticket " + ticket.getStatus(),
            "Support ticket \"" + ticket.getSubject() + "\" status changed to " + ticket.getStatus() + ".",
            "SUPPORT-STATUS:" + ticket.getId() + ":" + ticket.getStatus(),
            "SUPPORT_TICKET", ticket.getId(), "/support.html", NotificationPriority.NORMAL,
            simpleEmail("Support ticket status", "Support ticket \"" + ticket.getSubject() + "\" status changed to " + ticket.getStatus() + "."));
    }

    @Override
    public void sendSecurityNotification(User user, NotificationType type, String title, String message, String relatedReference) {
        notifyUser(user, type, title, message, relatedReference, "USER", user.getId(), "/profile.html", NotificationPriority.CRITICAL, simpleEmail(title, message));
    }

    @Override
    public void sendWalletNotification(WalletTransaction transaction) {
        NotificationType type = switch (transaction.getTransactionType()) {
            case TOP_UP -> NotificationType.WALLET_TOPUP_SUCCESS;
            case BOOKING_PAYMENT -> NotificationType.WALLET_PAYMENT;
            case REFUND -> NotificationType.WALLET_REFUND;
        };
        String message = walletMessage(transaction);
        notifyUser(transaction.getWallet().getUser(), type, walletTitle(type), message,
            "WALLET:" + transaction.getId(), "WALLET_TRANSACTION", transaction.getId(), "/wallet.html",
            NotificationPriority.NORMAL, simpleEmail(walletTitle(type), message));
    }

    @Override
    public PageResponse<NotificationResponse> sendManual(String actorEmail, ManualNotificationRequest request) {
        User actor = users.findByEmailIgnoreCase(actorEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<User> recipients = manualRecipients(request);
        if (recipients.isEmpty()) {
            return new PageResponse<>(List.of(), 0, 0, 0, 0, true);
        }
        List<NotificationResponse> responses = recipients.stream()
            .map(recipient -> notifyUser(
                recipient,
                request.notificationType(),
                request.title().trim(),
                request.message().trim(),
                "ADMIN:" + actor.getId() + ":" + request.targetType() + ":" + safe(request.targetId()) + ":" + hash(request.title() + request.message()),
                request.targetType().name(),
                request.targetId(),
                clean(request.actionUrl()),
                request.priority() == null ? NotificationPriority.NORMAL : request.priority(),
                simpleEmail(request.title().trim(), request.message().trim()),
                request.category() == null ? NotificationCategory.ADMIN : request.category()
            ))
            .filter(Objects::nonNull)
            .map(DtoMapper::notification)
            .toList();
        return PageResponse.from(new PageImpl<>(responses, Pageable.unpaged(), responses.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(String email, NotificationStatus status, NotificationCategory category, Pageable pageable) {
        var page = status != null && category != null
            ? notifications.findByUserEmailIgnoreCaseAndStatusAndCategoryAndArchivedFalseOrderBySentDateTimeDesc(email, status, category, pageable)
            : status != null
                ? notifications.findByUserEmailIgnoreCaseAndStatusAndArchivedFalseOrderBySentDateTimeDesc(email, status, pageable)
                : category != null
                    ? notifications.findByUserEmailIgnoreCaseAndCategoryAndArchivedFalseOrderBySentDateTimeDesc(email, category, pageable)
                    : notifications.findByUserEmailIgnoreCaseAndArchivedFalseOrderBySentDateTimeDesc(email, pageable);
        return PageResponse.from(page.map(DtoMapper::notification));
    }

    @Override
    public NotificationResponse markRead(String email, Long notificationId) {
        Notification notification = notificationForUser(email, notificationId);
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        return DtoMapper.notification(notification);
    }

    @Override
    public void markAllRead(String email) {
        notifications.findByUserEmailIgnoreCaseAndStatusAndArchivedFalseOrderBySentDateTimeDesc(email, NotificationStatus.SENT, Pageable.unpaged())
            .forEach(notification -> {
                notification.setStatus(NotificationStatus.READ);
                notification.setReadAt(LocalDateTime.now());
            });
    }

    @Override
    public void archive(String email, Long notificationId) {
        Notification notification = notificationForUser(email, notificationId);
        notification.setArchived(true);
        if (notification.getStatus() == NotificationStatus.SENT) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listAll(NotificationStatus status, Pageable pageable) {
        var page = status == null
            ? notifications.findAllByOrderBySentDateTimeDesc(pageable)
            : notifications.findByStatusOrderBySentDateTimeDesc(status, pageable);
        return PageResponse.from(page.map(DtoMapper::notification));
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notifications.countByUserEmailIgnoreCaseAndStatusAndArchivedFalse(email, NotificationStatus.SENT);
    }

    private Notification notifyUser(User recipient, NotificationType type, String title, String message, String relatedReference,
                                    String relatedEntityType, Long relatedEntityId, String actionUrl,
                                    NotificationPriority priority, String htmlEmail) {
        return notifyUser(recipient, type, title, message, relatedReference, relatedEntityType, relatedEntityId, actionUrl, priority, htmlEmail, categoryFor(type));
    }

    private Notification notifyUser(User recipient, NotificationType type, String title, String message, String relatedReference,
                                    String relatedEntityType, Long relatedEntityId, String actionUrl,
                                    NotificationPriority priority, String htmlEmail, NotificationCategory category) {
        if (recipient == null || recipient.getId() == null) {
            return null;
        }
        if (relatedReference != null && notifications.existsByUserIdAndNotificationTypeAndRelatedReference(recipient.getId(), type, relatedReference)) {
            log.info("Duplicate notification skipped for user {} type {} reference {}", recipient.getId(), type, relatedReference);
            return null;
        }

        PreferenceState preference = preferenceFor(recipient);
        boolean mandatory = mandatory(type, category, priority);
        boolean categoryEnabled = mandatory || categoryEnabled(preference, category);
        String channels = categoryEnabled ? channels(preference, recipient, mandatory) : "NONE";
        boolean emailFailed = false;
        boolean smsFailed = false;
        if (categoryEnabled && (mandatory || preference.emailEnabled())) {
            try {
                emailService.sendNotificationEmail(recipient.getEmail(), "SmartRoute Lanka - " + title, htmlEmail);
            } catch (Exception ex) {
                emailFailed = true;
                log.warn("Notification email failed for user {} type {}", recipient.getId(), type);
            }
        }
        if (categoryEnabled && (mandatory || preference.smsEnabled()) && recipient.getPhone() != null && !recipient.getPhone().isBlank()) {
            try {
                smsService.sendSms(recipient.getPhone(), title + ": " + message);
            } catch (Exception ex) {
                smsFailed = true;
                log.warn("Notification SMS failed for user {} type {}", recipient.getId(), type);
            }
        }

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCategory(category);
        notification.setRelatedReference(relatedReference);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setActionUrl(actionUrl);
        notification.setPriority(priority == null ? NotificationPriority.NORMAL : priority);
        notification.setDeliveryChannels(channels);
        notification.setStatus(statusFor(channels, emailFailed, smsFailed));
        notification.setSentDateTime(LocalDateTime.now());
        Notification saved = notifications.save(notification);
        streams.publish(saved);
        log.info("Notification dispatched: id={}, type={}, status={}, channels={}, recipient={}",
            saved.getId(), type, saved.getStatus(), saved.getDeliveryChannels(), recipient.getEmail());
        return saved;
    }

    private Set<User> manualRecipients(ManualNotificationRequest request) {
        Set<User> result = new LinkedHashSet<>();
        switch (request.targetType()) {
            case PASSENGER -> {
                Long id = requiredTargetId(request);
                result.add(passengers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Passenger not found")).getUser());
            }
            case SELECTED_PASSENGERS -> {
                if (request.targetIds() == null || request.targetIds().isEmpty()) {
                    throw new IllegalArgumentException("Passenger IDs are required");
                }
                passengers.findByIdIn(request.targetIds()).forEach(passenger -> result.add(passenger.getUser()));
            }
            case SCHEDULE -> {
                Long id = requiredTargetId(request);
                schedules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
                bookings.findByScheduleIdAndBookingStatusIn(id, AFFECTED_BOOKINGS).forEach(booking -> result.add(booking.getPassenger().getUser()));
            }
            case ROUTE -> {
                Long id = requiredTargetId(request);
                schedules.findByRouteId(id).forEach(schedule ->
                    bookings.findByScheduleIdAndBookingStatusIn(schedule.getId(), AFFECTED_BOOKINGS)
                        .forEach(booking -> result.add(booking.getPassenger().getUser())));
            }
            case ALL_PASSENGERS -> result.addAll(users.findByUserTypeAndActiveTrue(UserType.PASSENGER));
        }
        return result;
    }

    private Long requiredTargetId(ManualNotificationRequest request) {
        if (request.targetId() == null) {
            throw new IllegalArgumentException("Target ID is required for " + request.targetType());
        }
        return request.targetId();
    }

    private Notification notificationForUser(String email, Long notificationId) {
        Notification notification = notifications.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenActionException("Notification does not belong to user");
        }
        return notification;
    }

    private void forAffectedBookings(Schedule schedule, java.util.function.Consumer<Booking> consumer) {
        bookings.findByScheduleIdAndBookingStatusIn(schedule.getId(), AFFECTED_BOOKINGS).forEach(consumer);
    }

    private PreferenceState preferenceFor(User recipient) {
        return preferences.findByUserId(recipient.getId())
            .map(p -> new PreferenceState(
                p.isEmailEnabled(), p.isSmsEnabled(), p.isInAppEnabled(),
                p.isBookingUpdatesEnabled(), p.isPaymentUpdatesEnabled(), p.isScheduleChangesEnabled(),
                p.isTripRemindersEnabled(), p.isWalletUpdatesEnabled(), p.isSupportMessagesEnabled(),
                p.isSecurityAlertsEnabled(), p.isPromotionalEnabled()
            ))
            .orElse(new PreferenceState(true, false, true, true, true, true, true, true, true, true, false));
    }

    private String channels(PreferenceState preference, User recipient, boolean mandatory) {
        List<String> channels = new ArrayList<>();
        if (mandatory || preference.inAppEnabled()) channels.add("IN_APP");
        if (mandatory || preference.emailEnabled()) channels.add("EMAIL");
        if ((mandatory || preference.smsEnabled()) && recipient.getPhone() != null && !recipient.getPhone().isBlank()) channels.add("SMS");
        return channels.isEmpty() ? "NONE" : String.join(",", channels);
    }

    private NotificationStatus statusFor(String channels, boolean emailFailed, boolean smsFailed) {
        if ("NONE".equals(channels)) {
            return NotificationStatus.SKIPPED;
        }
        if (emailFailed && smsFailed && !channels.contains("IN_APP")) {
            return NotificationStatus.FAILED;
        }
        return NotificationStatus.SENT;
    }

    private boolean categoryEnabled(PreferenceState preference, NotificationCategory category) {
        return switch (category) {
            case BOOKING_UPDATES -> preference.bookingUpdatesEnabled();
            case PAYMENT_UPDATES -> preference.paymentUpdatesEnabled();
            case SCHEDULE_CHANGES -> preference.scheduleChangesEnabled();
            case TRIP_REMINDERS -> preference.tripRemindersEnabled();
            case WALLET_UPDATES -> preference.walletUpdatesEnabled();
            case SUPPORT_MESSAGES -> preference.supportMessagesEnabled();
            case SECURITY_ALERTS -> preference.securityAlertsEnabled();
            case PROMOTIONAL -> preference.promotionalEnabled();
            case ADMIN -> true;
        };
    }

    private boolean mandatory(NotificationType type, NotificationCategory category, NotificationPriority priority) {
        return priority == NotificationPriority.CRITICAL
            || category == NotificationCategory.SECURITY_ALERTS
            || type == NotificationType.SCHEDULE_CANCELLED
            || type == NotificationType.SCHEDULE_CANCELLATION
            || type == NotificationType.SCHEDULE_DELAYED;
    }

    private NotificationCategory categoryFor(NotificationType type) {
        return switch (type) {
            case PAYMENT_CONFIRMATION, PAYMENT_RECEIPT, PAYMENT_SUCCESS, PAYMENT_FAILED, REFUND_CONFIRMATION, REFUND_COMPLETED -> NotificationCategory.PAYMENT_UPDATES;
            case SCHEDULE_DELAY, SCHEDULE_DELAYED, SCHEDULE_CANCELLATION, SCHEDULE_CANCELLED, SCHEDULE_UPDATED, BUS_CHANGED, SEAT_CHANGED -> NotificationCategory.SCHEDULE_CHANGES;
            case BOOKING_REMINDER -> NotificationCategory.TRIP_REMINDERS;
            case WALLET_TOPUP_SUCCESS, WALLET_TOPUP_FAILED, WALLET_PAYMENT, WALLET_REFUND, WALLET_TRANSACTION_REVERSED, WALLET_LOW_BALANCE -> NotificationCategory.WALLET_UPDATES;
            case CUSTOMER_SUPPORT_RESPONSE, SUPPORT_TICKET_CREATED, SUPPORT_REPLY, SUPPORT_ASSIGNED, SUPPORT_STATUS_CHANGED, SUPPORT_RESOLVED, SUPPORT_CLOSED -> NotificationCategory.SUPPORT_MESSAGES;
            case PASSWORD_CHANGED, CARD_ADDED, CARD_REMOVED, ACCOUNT_DISABLED, ACCOUNT_REACTIVATED -> NotificationCategory.SECURITY_ALERTS;
            case ADMIN_BROADCAST -> NotificationCategory.ADMIN;
            default -> NotificationCategory.BOOKING_UPDATES;
        };
    }

    private String bookingEmail(Booking booking, String title, String note) {
        String seats = seatsLabel(booking);
        Schedule schedule = booking.getSchedule();
        return emailShell(title, """
            <p><strong>Booking reference:</strong> %s</p>
            <p><strong>Passenger:</strong> %s</p>
            <p><strong>Route:</strong> %s</p>
            <p><strong>Travel date:</strong> %s</p>
            <p><strong>Departure:</strong> %s</p>
            <p><strong>Arrival:</strong> %s</p>
            <p><strong>Bus:</strong> %s</p>
            <p><strong>Seats:</strong> %s</p>
            <p><strong>Total amount:</strong> LKR %s</p>
            %s
            """.formatted(
            escape(booking.getBookingReference()),
            escape(booking.getPassenger().getUser().getFullName()),
            escape(routeLabel(schedule)),
            escape(schedule.getTravelDate()),
            escape(schedule.getDepartureTime()),
            escape(schedule.getArrivalTime()),
            escape(busLabel(schedule)),
            escape(seats),
            escape(booking.getTotalAmount()),
            note == null ? "" : "<p>" + escape(note) + "</p>"
        ));
    }

    private String paymentReceiptEmail(Payment payment) {
        return emailShell("Payment receipt", """
            <p><strong>Receipt number:</strong> REC-%s</p>
            <p><strong>Booking reference:</strong> %s</p>
            <p><strong>Payment ID:</strong> %s</p>
            <p><strong>Amount:</strong> LKR %s</p>
            <p><strong>Payment method:</strong> %s</p>
            <p><strong>Transaction reference:</strong> %s</p>
            <p><strong>Payment date:</strong> %s</p>
            <p><strong>Payment status:</strong> %s</p>
            """.formatted(
            escape(payment.getId()),
            escape(payment.getBooking().getBookingReference()),
            escape(payment.getId()),
            escape(payment.getAmount()),
            escape(payment.getPaymentMethod()),
            escape(payment.getTransactionReference()),
            escape(payment.getPaymentDate()),
            escape(payment.getPaymentStatus())
        ));
    }

    private String scheduleEmail(Booking booking, String title, String note) {
        Schedule schedule = booking.getSchedule();
        return emailShell(title, """
            <p><strong>Booking reference:</strong> %s</p>
            <p><strong>Route:</strong> %s</p>
            <p><strong>Travel date:</strong> %s</p>
            <p><strong>Current departure:</strong> %s</p>
            <p><strong>Arrival:</strong> %s</p>
            <p><strong>Bus:</strong> %s</p>
            <p>%s</p>
            <p>Support: Contact SmartRoute Lanka support from your account.</p>
            """.formatted(
            escape(booking.getBookingReference()),
            escape(routeLabel(schedule)),
            escape(schedule.getTravelDate()),
            escape(schedule.getDepartureTime()),
            escape(schedule.getArrivalTime()),
            escape(busLabel(schedule)),
            escape(note)
        ));
    }

    private String refundEmail(Payment payment) {
        return emailShell("Refund confirmation", """
            <p><strong>Booking reference:</strong> %s</p>
            <p><strong>Refund amount:</strong> LKR %s</p>
            <p><strong>Refund method:</strong> %s</p>
            <p><strong>Refund reference:</strong> %s</p>
            <p><strong>Refund status:</strong> %s</p>
            <p><strong>Refund date:</strong> %s</p>
            """.formatted(
            escape(payment.getBooking().getBookingReference()),
            escape(payment.getRefundedAmount()),
            escape(payment.getPaymentMethod()),
            escape(payment.getTransactionReference()),
            escape(payment.getPaymentStatus()),
            escape(LocalDateTime.now())
        ));
    }

    private String simpleEmail(String title, String body) {
        return emailShell(title, "<p>" + escape(body) + "</p>");
    }

    private String emailShell(String title, String body) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto;border:1px solid #dbe3eb;border-radius:12px;overflow:hidden">
              <div style="background:#155bd7;color:white;padding:22px 26px">
                <h1 style="margin:0;font-size:22px">SmartRoute Lanka</h1>
                <p style="margin:6px 0 0;color:#dce9ff">%s</p>
              </div>
              <div style="padding:26px;color:#102c47">%s</div>
            </div>
            """.formatted(escape(title), body);
    }

    private String routeLabel(Schedule schedule) {
        return schedule.getRoute().getOrigin() + " to " + schedule.getRoute().getDestination();
    }

    private String busLabel(Schedule schedule) {
        return schedule.getBus() == null ? "-" : schedule.getBus().getRegistrationNo();
    }

    private String seatsLabel(Booking booking) {
        return booking.getBookingSeats().stream()
            .map(bookingSeat -> String.valueOf(bookingSeat.getSeat().getSeatNumber()))
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElse("-");
    }

    private String walletMessage(WalletTransaction tx) {
        return "Wallet transaction " + tx.getTransactionReference() + " (" + tx.getTransactionType() + ") for "
            + displayMoney(tx.getAmount()) + " is " + tx.getStatus() + ". New balance: " + displayMoney(tx.getBalanceAfter()) + ".";
    }

    private String walletTitle(NotificationType type) {
        return switch (type) {
            case WALLET_TOPUP_SUCCESS -> "Wallet top-up successful";
            case WALLET_PAYMENT -> "Wallet payment deducted";
            case WALLET_REFUND -> "Wallet refund credited";
            default -> "Wallet update";
        };
    }

    private String displayMoney(BigDecimal amount) {
        return "LKR " + (amount == null ? "0.00" : amount);
    }

    private String safeReason(String value) {
        String reason = clean(value);
        return reason == null ? "The payment provider declined the transaction." : "Reason: " + reason + ".";
    }

    private String safeDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String safe(Object value) {
        return String.valueOf(value == null ? "" : value);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int hash(Object value) {
        return String.valueOf(value).hashCode();
    }

    private Long staffId(SupportTicket ticket) {
        return ticket.getHandledByStaff() == null ? null : ticket.getHandledByStaff().getId();
    }

    private String escape(Object value) {
        return String.valueOf(value == null ? "" : value)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private record PreferenceState(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean inAppEnabled,
        boolean bookingUpdatesEnabled,
        boolean paymentUpdatesEnabled,
        boolean scheduleChangesEnabled,
        boolean tripRemindersEnabled,
        boolean walletUpdatesEnabled,
        boolean supportMessagesEnabled,
        boolean securityAlertsEnabled,
        boolean promotionalEnabled
    ) {
    }
}
