package lk.smartroute.repository;

import lk.smartroute.model.Notification;
import lk.smartroute.model.NotificationStatus;
import lk.smartroute.model.NotificationType;
import lk.smartroute.model.NotificationCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserEmailIgnoreCaseOrderBySentDateTimeDesc(String email, Pageable pageable);

    Page<Notification> findByUserEmailIgnoreCaseAndArchivedFalseOrderBySentDateTimeDesc(String email, Pageable pageable);

    Page<Notification> findByUserEmailIgnoreCaseAndStatusAndArchivedFalseOrderBySentDateTimeDesc(String email, NotificationStatus status, Pageable pageable);

    Page<Notification> findByUserEmailIgnoreCaseAndCategoryAndArchivedFalseOrderBySentDateTimeDesc(String email, NotificationCategory category, Pageable pageable);

    Page<Notification> findByUserEmailIgnoreCaseAndStatusAndCategoryAndArchivedFalseOrderBySentDateTimeDesc(String email, NotificationStatus status, NotificationCategory category, Pageable pageable);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByNotificationType(NotificationType notificationType);

    List<Notification> findBySentDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    long countByUserEmailIgnoreCaseAndStatusAndArchivedFalse(String email, NotificationStatus status);

    Page<Notification> findAllByOrderBySentDateTimeDesc(Pageable pageable);

    Page<Notification> findByStatusOrderBySentDateTimeDesc(NotificationStatus status, Pageable pageable);

    boolean existsByNotificationTypeAndRelatedReference(NotificationType notificationType, String relatedReference);

    boolean existsByUserIdAndNotificationTypeAndRelatedReference(Long userId, NotificationType notificationType, String relatedReference);
}
