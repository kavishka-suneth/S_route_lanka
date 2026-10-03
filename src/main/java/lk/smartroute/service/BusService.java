package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.AvailabilityStatus;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BusService {
    PageResponse<BusResponse> list(Boolean active, AvailabilityStatus availabilityStatus, Pageable pageable);

    List<BusResponse> available(LocalDate travelDate, LocalTime departureTime, LocalTime arrivalTime, Long excludeScheduleId);

    BusResponse get(Long id);

    BusResponse create(BusRequest request, String actorEmail);

    BusResponse update(Long id, BusRequest request, String actorEmail);

    void delete(Long id, String actorEmail);
}
