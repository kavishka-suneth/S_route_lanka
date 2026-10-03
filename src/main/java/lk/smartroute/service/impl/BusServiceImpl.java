package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.AvailabilityStatus;
import lk.smartroute.model.Bus;
import lk.smartroute.model.BusStatus;
import lk.smartroute.model.Seat;
import lk.smartroute.model.SeatType;
import lk.smartroute.repository.BusRepository;
import lk.smartroute.repository.ScheduleRepository;
import lk.smartroute.service.BusService;
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
public class BusServiceImpl implements BusService {
    private final BusRepository buses;
    private final ScheduleRepository schedules;

    public BusServiceImpl(BusRepository buses, ScheduleRepository schedules) {
        this.buses = buses;
        this.schedules = schedules;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusResponse> list(Boolean active, AvailabilityStatus availabilityStatus, Pageable pageable) {
        Page<Bus> page = buses.searchForManagement(active, availabilityStatus, pageable);
        return PageResponse.from(page.map(this::busResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusResponse> available(LocalDate travelDate, LocalTime departureTime, LocalTime arrivalTime, Long excludeScheduleId) {
        validateAvailabilityWindow(travelDate, departureTime, arrivalTime);
        return buses.findAvailableDuring(travelDate, departureTime, arrivalTime, excludeScheduleId).stream()
            .map(this::busResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BusResponse get(Long id) {
        return busResponse(busById(id));
    }

    @Override
    public BusResponse create(BusRequest request, String actorEmail) {
        if (buses.existsByRegistrationNoIgnoreCase(request.registrationNo())) {
            throw new ConflictException("Bus registration number already exists");
        }
        Bus bus = new Bus();
        apply(bus, request);
        syncSeats(bus, request.capacity());
        Bus saved = buses.save(bus);
        return busResponse(saved);
    }

    @Override
    public BusResponse update(Long id, BusRequest request, String actorEmail) {
        Bus bus = busById(id);
        if (!bus.getRegistrationNo().equalsIgnoreCase(request.registrationNo())
            && buses.existsByRegistrationNoIgnoreCase(request.registrationNo())) {
            throw new ConflictException("Bus registration number already exists");
        }
        assertNoFutureAssignmentForUnavailableState(bus.getId(), request);
        apply(bus, request);
        syncSeats(bus, request.capacity());
        return busResponse(bus);
    }

    @Override
    public void delete(Long id, String actorEmail) {
        Bus bus = busById(id);
        assertNoFutureActiveSchedules(bus.getId(), "Bus has future active schedules. Cancel or reassign those schedules before deactivating this bus.");
        bus.setStatus(BusStatus.INACTIVE);
        bus.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
    }

    private void apply(Bus bus, BusRequest request) {
        AvailabilityStatus availabilityStatus = request.availabilityStatus() == null ? AvailabilityStatus.AVAILABLE : request.availabilityStatus();
        if (availabilityStatus == AvailabilityStatus.ASSIGNED) {
            throw new IllegalArgumentException("ASSIGNED is derived from schedules and cannot be selected manually");
        }
        bus.setRegistrationNo(request.registrationNo().trim().toUpperCase());
        bus.setCapacity(request.capacity());
        bus.setBusType(request.busType());
        bus.setAvailabilityStatus(availabilityStatus);
        bus.setStatus(request.status() == null ? BusStatus.ACTIVE : request.status());
    }

    private void assertNoFutureAssignmentForUnavailableState(Long busId, BusRequest request) {
        AvailabilityStatus availabilityStatus = request.availabilityStatus() == null ? AvailabilityStatus.AVAILABLE : request.availabilityStatus();
        BusStatus status = request.status() == null ? BusStatus.ACTIVE : request.status();
        if (status != BusStatus.ACTIVE || availabilityStatus == AvailabilityStatus.UNAVAILABLE || availabilityStatus == AvailabilityStatus.MAINTENANCE) {
            assertNoFutureActiveSchedules(busId, "Bus has future active schedules. Cancel or reassign those schedules before making this bus unavailable.");
        }
    }

    private void assertNoFutureActiveSchedules(Long busId, String message) {
        LocalDateTime now = LocalDateTime.now();
        if (buses.countFutureActiveSchedules(busId, now.toLocalDate(), now.toLocalTime()) > 0) {
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

    private BusResponse busResponse(Bus bus) {
        LocalDateTime now = LocalDateTime.now();
        ScheduleSummary summary = nextBusAssignment(bus.getId(), now);
        return DtoMapper.bus(bus, summary.assignedNow(), summary.assignment());
    }

    private ScheduleSummary nextBusAssignment(Long busId, LocalDateTime now) {
        return schedules.findCurrentOrNextActiveBusAssignment(busId, now.toLocalDate(), now.toLocalTime(), PageRequest.of(0, 1)).stream()
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

    private void syncSeats(Bus bus, int capacity) {
        bus.getSeats().removeIf(seat -> seat.getSeatNumber() > capacity);
        java.util.Set<Integer> existingNumbers = bus.getSeats().stream()
            .map(Seat::getSeatNumber)
            .collect(java.util.stream.Collectors.toSet());
        for (int seatNumber = 1; seatNumber <= capacity; seatNumber++) {
            if (!existingNumbers.contains(seatNumber)) {
                Seat seat = new Seat();
                seat.setBus(bus);
                seat.setSeatNumber(seatNumber);
                seat.setSeatType(SeatType.REGULAR);
                bus.getSeats().add(seat);
            }
        }
    }

    private Bus busById(Long id) {
        return buses.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));
    }
}
