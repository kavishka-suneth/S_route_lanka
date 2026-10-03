package lk.smartroute.repository;

import lk.smartroute.model.Payment;
import lk.smartroute.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingIdOrderByPaymentDateDesc(Long bookingId);

    Optional<Payment> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);

    Optional<Payment> findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(Long bookingId, PaymentStatus paymentStatus);

    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
        select p
        from Payment p
        join p.booking b
        join b.passenger passenger
        join passenger.user u
        where (:status is null or p.paymentStatus = :status)
          and (
            :keyword is null
            or :keyword = ''
            or lower(p.transactionReference) like lower(concat('%', :keyword, '%'))
            or lower(p.paymentMethod) like lower(concat('%', :keyword, '%'))
            or lower(b.bookingReference) like lower(concat('%', :keyword, '%'))
            or lower(u.firstName) like lower(concat('%', :keyword, '%'))
            or lower(u.lastName) like lower(concat('%', :keyword, '%'))
            or lower(u.email) like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Payment> searchPayments(
        @Param("status") PaymentStatus status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS")
    BigDecimal totalSuccessfulRevenue();

    @Query("""
        select coalesce(sum(p.amount), 0)
        from Payment p
        where p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
          and p.paymentDate between :startDate and :endDate
        """)
    BigDecimal revenueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("""
        select p.paymentMethod as paymentMethod, coalesce(sum(p.amount), 0) as revenue
        from Payment p
        where p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
        group by p.paymentMethod
        """)
    List<RevenueByPaymentMethod> revenueGroupedByPaymentMethod();

    @Query("""
        select p.paymentMethod as paymentMethod, coalesce(sum(p.amount), 0) as revenue, count(p) as successfulPayments
        from Payment p
        where p.paymentStatus = lk.smartroute.model.PaymentStatus.SUCCESS
          and (:from is null or p.paymentDate >= :from)
          and (:to is null or p.paymentDate <= :to)
        group by p.paymentMethod
        order by revenue desc, p.paymentMethod
        """)
    List<RevenueByPaymentMethod> revenueGroupedByPaymentMethodBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select coalesce(sum(p.amount), 0)
        from Payment p
        where p.paymentStatus = :status
          and (:from is null or p.paymentDate >= :from)
          and (:to is null or p.paymentDate <= :to)
        """)
    BigDecimal sumAmountByStatusBetween(@Param("status") PaymentStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select count(p)
        from Payment p
        where p.paymentStatus = :status
          and (:from is null or p.paymentDate >= :from)
          and (:to is null or p.paymentDate <= :to)
        """)
    long countByStatusBetween(@Param("status") PaymentStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByPaymentStatusAndPaymentDateBetween(PaymentStatus paymentStatus, LocalDateTime startDate, LocalDateTime endDate);

    interface RevenueByPaymentMethod {
        String getPaymentMethod();
        BigDecimal getRevenue();
        long getSuccessfulPayments();
    }
}
