package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.AvailabilityStatus;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DriverService {
    PageResponse<DriverResponse> list(Boolean active, AvailabilityStatus availabilityStatus, Pageable pageable);

    List<DriverResponse> available(LocalDate travelDate, LocalTime departureTime, LocalTime arrivalTime, Long excludeScheduleId);

    DriverResponse get(Long id);

    DriverResponse create(DriverRequest request, String actorEmail);

    DriverResponse update(Long id, DriverRequest request, String actorEmail);

    void delete(Long id, String actorEmail);
}
