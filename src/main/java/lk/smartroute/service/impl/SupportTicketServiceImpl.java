package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.*;
import lk.smartroute.repository.StaffRepository;
import lk.smartroute.repository.SupportTicketRepository;
import lk.smartroute.repository.UserRepository;
import lk.smartroute.service.NotificationService;
import lk.smartroute.service.SupportTicketService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {
    private final SupportTicketRepository tickets;
    private final UserRepository users;
    private final StaffRepository staffRepository;
    private final NotificationService notifications;

    public SupportTicketServiceImpl(
        SupportTicketRepository tickets,
        UserRepository users,
        StaffRepository staffRepository,
        NotificationService notifications
    ) {
        this.tickets = tickets;
        this.users = users;
        this.staffRepository = staffRepository;
        this.notifications = notifications;
    }

    @Override
    public SupportTicketResponse create(String email, SupportTicketRequest request) {
        User user = userByEmail(email);
        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setCreatedDate(LocalDateTime.now());
        SupportTicket saved = tickets.save(ticket);
        notifications.sendSupportTicketCreated(saved);
        return DtoMapper.supportTicket(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> listMine(String email, Pageable pageable) {
        return PageResponse.from(tickets.findByUserEmailIgnoreCaseOrderByCreatedDateDesc(email, pageable).map(DtoMapper::supportTicket));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> listAll(SupportTicketStatus status, Boolean unassigned, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return PageResponse.from(tickets.searchBySubjectOrDescription(keyword, pageable).map(DtoMapper::supportTicket));
        }
        if (Boolean.TRUE.equals(unassigned)) {
            return PageResponse.from(tickets.findByHandledByStaffIsNull(pageable).map(DtoMapper::supportTicket));
        }
        if (status != null) {
            return PageResponse.from(tickets.findByStatus(status, pageable).map(DtoMapper::supportTicket));
        }
        return PageResponse.from(tickets.findAll(pageable).map(DtoMapper::supportTicket));
    }

    @Override
    public SupportTicketResponse respond(Long id, SupportTicketResponseRequest request, String staffEmail) {
        SupportTicket ticket = ticketById(id);
        SupportTicketStatus oldStatus = ticket.getStatus();
        Long oldStaffId = ticket.getHandledByStaff() == null ? null : ticket.getHandledByStaff().getId();
        Staff staff = request.handledByStaffId() == null
            ? staffRepository.findByUserEmailIgnoreCase(staffEmail).orElseThrow(() -> new ResourceNotFoundException("Staff member not found"))
            : staffRepository.findById(request.handledByStaffId()).orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        ticket.setHandledByStaff(staff);
        ticket.setStatus(request.status());
        if (request.response() != null && !request.response().isBlank()) {
            ticket.setDescription(ticket.getDescription() + "\n\nStaff response: " + request.response().trim());
        }
        if (!staff.getId().equals(oldStaffId)) {
            notifications.sendSupportTicketAssigned(ticket);
        }
        if (request.response() != null && !request.response().isBlank()) {
            notifications.sendSupportTicketResponse(ticket);
        }
        if (oldStatus != ticket.getStatus()) {
            notifications.sendSupportTicketStatusChanged(ticket);
        }
        return DtoMapper.supportTicket(ticket);
    }

    @Override
    public SupportTicketResponse updateStatus(Long id, SupportTicketStatusUpdateRequest request) {
        SupportTicket ticket = ticketById(id);
        SupportTicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(request.status());
        if (oldStatus != ticket.getStatus()) {
            notifications.sendSupportTicketStatusChanged(ticket);
        }
        return DtoMapper.supportTicket(ticket);
    }

    private User userByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private SupportTicket ticketById(Long id) {
        return tickets.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
    }
}
