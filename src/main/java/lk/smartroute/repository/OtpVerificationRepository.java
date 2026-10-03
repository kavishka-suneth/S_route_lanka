package lk.smartroute.repository;
import lk.smartroute.model.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface OtpVerificationRepository extends JpaRepository<OtpVerification,Long>{
 long deleteByExpiresAtBefore(java.time.LocalDateTime cutoff);
 Optional<OtpVerification> findFirstByEmailIgnoreCaseAndPurposeAndOperationKeyAndUsedFalseOrderByCreatedAtDesc(String email,OtpPurpose purpose,String operationKey);
 Optional<OtpVerification> findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email,OtpPurpose purpose);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from OtpVerification o where lower(o.email)=lower(:email) and o.purpose=:purpose and o.operationKey=:key and o.used=false order by o.createdAt desc")
 java.util.List<OtpVerification> findActiveForUpdate(@Param("email")String email,@Param("purpose")OtpPurpose purpose,@Param("key")String key);
 @Modifying @Query("update OtpVerification o set o.used=true where lower(o.email)=lower(:email) and o.purpose=:purpose and o.operationKey=:key and o.used=false")
 int invalidate(@Param("email")String email,@Param("purpose")OtpPurpose purpose,@Param("key")String key);
 @Modifying @Query("update OtpVerification o set o.used=true where lower(o.email)=lower(:email) and o.purpose=:purpose and o.used=false")
 int invalidateAll(@Param("email")String email,@Param("purpose")OtpPurpose purpose);
}
