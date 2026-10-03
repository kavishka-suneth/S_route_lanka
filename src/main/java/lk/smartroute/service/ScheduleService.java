package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.ScheduleStatus;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface ScheduleService {
    PageResponse<ScheduleResponse> search(String origin, String destination, LocalDate travelDate, Pageable pageable);

    PageResponse<ScheduleResponse> upcoming(Pageable pageable);

    PageResponse<ScheduleResponse> list(Pageable pageable);

    ScheduleResponse get(Long id);

    ScheduleResponse create(ScheduleRequest request, String actorEmail);

    ScheduleResponse update(Long id, ScheduleRequest request, String actorEmail);

    ScheduleResponse updateStatus(Long id, ScheduleStatus status, String actorEmail);

    default ScheduleResponse updateStatus(Long id, ScheduleStatusUpdateRequest request, String actorEmail) {
        return updateStatus(id, request.status(), actorEmail);
    }

    void delete(Long id, String actorEmail);
}
