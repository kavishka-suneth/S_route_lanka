package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.NotificationPreferenceRequest;
import lk.smartroute.dto.ApiDtos.NotificationPreferenceResponse;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.NotificationPreference;
import lk.smartroute.model.User;
import lk.smartroute.repository.NotificationPreferenceRepository;
import lk.smartroute.repository.UserRepository;
import lk.smartroute.service.NotificationPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {
    private final NotificationPreferenceRepository preferences;
    private final UserRepository users;

    public NotificationPreferenceServiceImpl(NotificationPreferenceRepository preferences, UserRepository users) {
        this.preferences = preferences;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse get(String email) {
        return response(preferenceFor(email));
    }

    @Override
    public NotificationPreferenceResponse update(String email, NotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceFor(email);
        preference.setEmailEnabled(request.emailEnabled());
        preference.setSmsEnabled(request.smsEnabled());
        preference.setInAppEnabled(request.inAppEnabled());
        if (request.bookingUpdatesEnabled() != null) preference.setBookingUpdatesEnabled(request.bookingUpdatesEnabled());
        if (request.paymentUpdatesEnabled() != null) preference.setPaymentUpdatesEnabled(request.paymentUpdatesEnabled());
        if (request.scheduleChangesEnabled() != null) preference.setScheduleChangesEnabled(request.scheduleChangesEnabled());
        if (request.tripRemindersEnabled() != null) preference.setTripRemindersEnabled(request.tripRemindersEnabled());
        if (request.walletUpdatesEnabled() != null) preference.setWalletUpdatesEnabled(request.walletUpdatesEnabled());
        if (request.supportMessagesEnabled() != null) preference.setSupportMessagesEnabled(request.supportMessagesEnabled());
        if (request.securityAlertsEnabled() != null && request.securityAlertsEnabled()) {
            preference.setSecurityAlertsEnabled(true);
        }
        if (request.promotionalEnabled() != null) preference.setPromotionalEnabled(request.promotionalEnabled());
        return response(preference);
    }

    private NotificationPreference preferenceFor(String email) {
        return preferences.findByUserEmailIgnoreCase(email)
            .orElseGet(() -> {
                User user = users.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                NotificationPreference preference = new NotificationPreference();
                preference.setUser(user);
                preference.setEmailEnabled(true);
                preference.setSmsEnabled(false);
                preference.setInAppEnabled(true);
                preference.setBookingUpdatesEnabled(true);
                preference.setPaymentUpdatesEnabled(true);
                preference.setScheduleChangesEnabled(true);
                preference.setTripRemindersEnabled(true);
                preference.setWalletUpdatesEnabled(true);
                preference.setSupportMessagesEnabled(true);
                preference.setSecurityAlertsEnabled(true);
                preference.setPromotionalEnabled(false);
                return preferences.save(preference);
            });
    }

    private NotificationPreferenceResponse response(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
            preference.isEmailEnabled(),
            preference.isSmsEnabled(),
            preference.isInAppEnabled(),
            preference.isBookingUpdatesEnabled(),
            preference.isPaymentUpdatesEnabled(),
            preference.isScheduleChangesEnabled(),
            preference.isTripRemindersEnabled(),
            preference.isWalletUpdatesEnabled(),
            preference.isSupportMessagesEnabled(),
            true,
            preference.isPromotionalEnabled()
        );
    }
}
