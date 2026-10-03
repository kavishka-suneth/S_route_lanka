package lk.smartroute.service.impl;

import lk.smartroute.model.Notification;
import lk.smartroute.service.NotificationStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseNotificationStreamService implements NotificationStreamService {
    private static final Logger log = LoggerFactory.getLogger(SseNotificationStreamService.class);
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.computeIfAbsent(email.toLowerCase(), key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(email, emitter));
        emitter.onTimeout(() -> remove(email, emitter));
        emitter.onError(error -> remove(email, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            remove(email, emitter);
        }
        return emitter;
    }

    @Override
    public void publish(Notification notification) {
        String email = notification.getUser().getEmail().toLowerCase();
        List<SseEmitter> userEmitters = emitters.getOrDefault(email, List.of());
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .id(String.valueOf(notification.getId()))
                    .data(Map.of(
                        "notificationId", notification.getId(),
                        "title", notification.getTitle(),
                        "category", notification.getCategory().name(),
                        "status", notification.getStatus().name()
                    )));
            } catch (Exception ex) {
                log.debug("Removing stale notification SSE emitter for user {}", notification.getUser().getId());
                remove(email, emitter);
            }
        }
    }

    private void remove(String email, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(email.toLowerCase());
        if (userEmitters != null) {
            userEmitters.remove(emitter);
        }
    }
}

