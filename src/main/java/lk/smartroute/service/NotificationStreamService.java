package lk.smartroute.service;

import lk.smartroute.model.Notification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationStreamService {
    SseEmitter subscribe(String email);

    void publish(Notification notification);
}

