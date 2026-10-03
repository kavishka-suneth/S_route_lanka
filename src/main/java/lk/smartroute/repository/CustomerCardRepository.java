package lk.smartroute.repository;
import lk.smartroute.model.CustomerCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CustomerCardRepository extends JpaRepository<CustomerCard,Long>{
 List<CustomerCard> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);
 Optional<CustomerCard> findByIdAndUserEmailIgnoreCase(Long id,String email);
 boolean existsByUserIdAndCardToken(Long userId,String token);
}
