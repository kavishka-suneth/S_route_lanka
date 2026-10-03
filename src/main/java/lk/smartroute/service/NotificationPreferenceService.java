package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.NotificationPreferenceRequest;
import lk.smartroute.dto.ApiDtos.NotificationPreferenceResponse;

public interface NotificationPreferenceService {
    NotificationPreferenceResponse get(String email);

    NotificationPreferenceResponse update(String email, NotificationPreferenceRequest request);
}
