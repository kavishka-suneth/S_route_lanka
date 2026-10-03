package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.SupportTicketStatus;
import org.springframework.data.domain.Pageable;

public interface SupportTicketService {
    SupportTicketResponse create(String email, SupportTicketRequest request);

    PageResponse<SupportTicketResponse> listMine(String email, Pageable pageable);

    PageResponse<SupportTicketResponse> listAll(SupportTicketStatus status, Boolean unassigned, String keyword, Pageable pageable);

    SupportTicketResponse respond(Long id, SupportTicketResponseRequest request, String staffEmail);

    SupportTicketResponse updateStatus(Long id, SupportTicketStatusUpdateRequest request);
}
