package lk.smartroute.repository;
import lk.smartroute.model.Wallet;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface WalletRepository extends JpaRepository<Wallet,Long>{
 Optional<Wallet> findByUserEmailIgnoreCase(String email);
 Optional<Wallet> findByUserId(Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Wallet w where w.id=:id") Optional<Wallet> findByIdForUpdate(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Wallet w where lower(w.user.email)=lower(:email)") Optional<Wallet> findByEmailForUpdate(String email);
}
