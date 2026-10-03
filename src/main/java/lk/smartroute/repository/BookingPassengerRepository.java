package lk.smartroute.repository;

import lk.smartroute.model.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {
    List<BookingPassenger> findByBookingIdOrderBySeatNumber(Long bookingId);
}
