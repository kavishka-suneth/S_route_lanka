package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.NotificationPreferenceRequest;
import lk.smartroute.dto.ApiDtos.NotificationPreferenceResponse;
import lk.smartroute.service.NotificationPreferenceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {
    private final NotificationPreferenceService preferences;

    public NotificationPreferenceController(NotificationPreferenceService preferences) {
        this.preferences = preferences;
    }

    @GetMapping
    public NotificationPreferenceResponse get(Authentication authentication) {
        return preferences.get(authentication.getName());
    }

    @PutMapping
    public NotificationPreferenceResponse update(Authentication authentication, @Valid @RequestBody NotificationPreferenceRequest request) {
        return preferences.update(authentication.getName(), request);
    }
}
