package lk.smartroute.repository;
import lk.smartroute.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,Long>{
 Page<WalletTransaction> findByWalletUserEmailIgnoreCaseOrderByCreatedAtDesc(String email,Pageable pageable);
 Optional<WalletTransaction> findByBookingIdAndTransactionType(Long bookingId,WalletTransactionType type);
 boolean existsByTransactionReference(String ref);
}
