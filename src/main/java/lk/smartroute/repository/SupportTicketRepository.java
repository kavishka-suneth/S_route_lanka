package lk.smartroute.repository;

import lk.smartroute.model.SupportTicket;
import lk.smartroute.model.SupportTicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findByUserEmailIgnoreCaseOrderByCreatedDateDesc(String email, Pageable pageable);

    Page<SupportTicket> findByHandledByStaffId(Long staffId, Pageable pageable);

    boolean existsByHandledByStaffId(Long staffId);

    Page<SupportTicket> findByStatus(SupportTicketStatus status, Pageable pageable);

    Page<SupportTicket> findByHandledByStaffIsNull(Pageable pageable);

    List<SupportTicket> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
        select t
        from SupportTicket t
        where lower(t.subject) like lower(concat('%', :keyword, '%'))
           or lower(t.description) like lower(concat('%', :keyword, '%'))
        """)
    Page<SupportTicket> searchBySubjectOrDescription(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(SupportTicketStatus status);

    @Query("""
        select t.handledByStaff.id as staffId, count(t) as ticketCount
        from SupportTicket t
        where t.handledByStaff is not null
        group by t.handledByStaff.id
        """)
    List<TicketCountByStaff> countTicketsHandledByStaff();

    interface TicketCountByStaff {
        Long getStaffId();
        long getTicketCount();
    }
}
