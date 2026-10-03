package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.AvailabilityStatus;
import lk.smartroute.model.Driver;
import lk.smartroute.repository.DriverRepository;
import lk.smartroute.repository.ScheduleRepository;
import lk.smartroute.service.DriverService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class DriverServiceImpl implements DriverService {
    private final DriverRepository drivers;
    private final ScheduleRepository schedules;

    public DriverServiceImpl(DriverRepository drivers, ScheduleRepository schedules) {
        this.drivers = drivers;
        this.schedules = schedules;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> list(Boolean active, AvailabilityStatus availabilityStatus, Pageable pageable) {
        Page<Driver> page = drivers.searchForManagement(active, availabilityStatus, pageable);
        return PageResponse.from(page.map(this::driverResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> available(LocalDate travelDate, LocalTime departureTime, LocalTime arrivalTime, Long excludeScheduleId) {
        validateAvailabilityWindow(travelDate, departureTime, arrivalTime);
        return drivers.findAvailableDuring(travelDate, departureTime, arrivalTime, excludeScheduleId).stream()
            .map(this::driverResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse get(Long id) {
        return driverResponse(driverById(id));
    }

    @Override
    public DriverResponse create(DriverRequest request, String actorEmail) {
        if (drivers.existsByLicenseNoIgnoreCase(request.licenseNo())) {
            throw new ConflictException("Driver license number already exists");
        }
        Driver driver = new Driver();
        apply(driver, request);
        Driver saved = drivers.save(driver);
        return driverResponse(saved);
    }

    @Override
    public DriverResponse update(Long id, DriverRequest request, String actorEmail) {
        Driver driver = driverById(id);
        if (!driver.getLicenseNo().equalsIgnoreCase(request.licenseNo())
            && drivers.existsByLicenseNoIgnoreCase(request.licenseNo())) {
            throw new ConflictException("Driver license number already exists");
        }
        assertNoFutureAssignmentForUnavailableState(driver.getId(), request);
        apply(driver, request);
        return driverResponse(driver);
    }

    @Override
    public void delete(Long id, String actorEmail) {
        Driver driver = driverById(id);
        assertNoFutureActiveSchedules(driver.getId(), "Driver has future active schedules. Cancel or reassign those schedules before making this driver unavailable.");
        driver.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
    }

    private void apply(Driver driver, DriverRequest request) {
        AvailabilityStatus availabilityStatus = request.availabilityStatus() == null ? AvailabilityStatus.AVAILABLE : request.availabilityStatus();
        if (availabilityStatus == AvailabilityStatus.ASSIGNED) {
            throw new IllegalArgumentException("ASSIGNED is derived from schedules and cannot be selected manually");
        }
        driver.setFirstName(request.firstName().trim());
        driver.setLastName(request.lastName().trim());
        driver.setLicenseNo(request.licenseNo().trim().toUpperCase());
        driver.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        driver.setAvailabilityStatus(availabilityStatus);
    }

    private void assertNoFutureAssignmentForUnavailableState(Long driverId, DriverRequest request) {
        AvailabilityStatus availabilityStatus = request.availabilityStatus() == null ? AvailabilityStatus.AVAILABLE : request.availabilityStatus();
        if (availabilityStatus == AvailabilityStatus.UNAVAILABLE || availabilityStatus == AvailabilityStatus.MAINTENANCE) {
            assertNoFutureActiveSchedules(driverId, "Driver has future active schedules. Cancel or reassign those schedules before making this driver unavailable.");
        }
    }

    private void assertNoFutureActiveSchedules(Long driverId, String message) {
        LocalDateTime now = LocalDateTime.now();
        if (drivers.countFutureActiveSchedules(driverId, now.toLocalDate(), now.toLocalTime()) > 0) {
            throw new ConflictException(message);
        }
    }

    private void validateAvailabilityWindow(LocalDate travelDate, LocalTime departureTime, LocalTime arrivalTime) {
        if (travelDate == null || departureTime == null || arrivalTime == null) {
            throw new IllegalArgumentException("travelDate, departureTime and arrivalTime are required");
        }
        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }
        if (!LocalDateTime.of(travelDate, departureTime).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Departure time must be in the future");
        }
    }

    private DriverResponse driverResponse(Driver driver) {
        LocalDateTime now = LocalDateTime.now();
        ScheduleSummary summary = nextDriverAssignment(driver.getId(), now);
        return DtoMapper.driver(driver, summary.assignedNow(), summary.assignment());
    }

    private ScheduleSummary nextDriverAssignment(Long driverId, LocalDateTime now) {
        return schedules.findCurrentOrNextActiveDriverAssignment(driverId, now.toLocalDate(), now.toLocalTime(), PageRequest.of(0, 1)).stream()
            .findFirst()
            .map(schedule -> new ScheduleSummary(isAssignedNow(schedule, now), DtoMapper.assignment(schedule)))
            .orElse(new ScheduleSummary(false, null));
    }

    private boolean isAssignedNow(lk.smartroute.model.Schedule schedule, LocalDateTime now) {
        return schedule.getTravelDate().equals(now.toLocalDate())
            && !schedule.getDepartureTime().isAfter(now.toLocalTime())
            && schedule.getArrivalTime().isAfter(now.toLocalTime());
    }

    private record ScheduleSummary(boolean assignedNow, AssignmentSummaryResponse assignment) {
    }

    private Driver driverById(Long id) {
        return drivers.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
    }
}
