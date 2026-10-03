package lk.smartroute.repository;

import lk.smartroute.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    Optional<Passenger> findByUserEmailIgnoreCase(String email);

    Optional<Passenger> findByUserId(Long userId);

    List<Passenger> findByIdIn(Collection<Long> ids);
}
