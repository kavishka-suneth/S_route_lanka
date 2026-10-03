package lk.smartroute.controller;

import lk.smartroute.dto.ApiDtos.NotificationResponse;
import lk.smartroute.dto.ApiDtos.PageResponse;
import lk.smartroute.dto.ApiDtos.ManualNotificationRequest;
import lk.smartroute.service.NotificationService;
import lk.smartroute.service.NotificationStreamService;
import lk.smartroute.model.NotificationCategory;
import lk.smartroute.model.NotificationStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final NotificationStreamService streams;

    public NotificationController(NotificationService notifications, NotificationStreamService streams) {
        this.notifications = notifications;
        this.streams = streams;
    }

    @GetMapping("/my")
    public PageResponse<NotificationResponse> mine(
        Authentication authentication,
        @RequestParam(required = false) NotificationStatus status,
        @RequestParam(required = false) NotificationCategory category,
        @PageableDefault(sort = "sentDateTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return notifications.listForUser(authentication.getName(), status, category, pageable);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(Authentication authentication, @PathVariable Long id) {
        return notifications.markRead(authentication.getName(), id);
    }

    @PatchMapping("/my/read-all")
    public void markAllRead(Authentication authentication) {
        notifications.markAllRead(authentication.getName());
    }

    @DeleteMapping("/{id}")
    public void archive(Authentication authentication, @PathVariable Long id) {
        notifications.archive(authentication.getName(), id);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication authentication) {
        return notifications.unreadCount(authentication.getName());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        return streams.subscribe(authentication.getName());
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','OPERATIONS_MANAGER','CUSTOMER_SERVICE','TRANSPORT_COORDINATOR')")
    public PageResponse<NotificationResponse> manual(Authentication authentication, @Valid @RequestBody ManualNotificationRequest request) {
        return notifications.sendManual(authentication.getName(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public PageResponse<NotificationResponse> listAll(
        @RequestParam(required = false) NotificationStatus status,
        @PageableDefault(sort = "sentDateTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return notifications.listAll(status, pageable);
    }
}
