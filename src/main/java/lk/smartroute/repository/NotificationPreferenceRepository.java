package lk.smartroute.repository;

import lk.smartroute.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserEmailIgnoreCase(String email);
}
