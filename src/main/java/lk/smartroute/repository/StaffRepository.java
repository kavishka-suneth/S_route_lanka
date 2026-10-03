package lk.smartroute.repository;

import lk.smartroute.model.Staff;
import lk.smartroute.model.StaffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUserEmailIgnoreCase(String email);

    Optional<Staff> findByUserId(Long userId);

    Page<Staff> findByStaffType(StaffType staffType, Pageable pageable);

    Page<Staff> findByStaffTypeAndUserActive(StaffType staffType, boolean active, Pageable pageable);

    long countByStaffType(StaffType staffType);

    long countByStaffTypeAndUserActive(StaffType staffType, boolean active);
}
