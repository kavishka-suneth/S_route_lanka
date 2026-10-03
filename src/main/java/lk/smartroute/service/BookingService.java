package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.AvailableSeatsResponse;
import lk.smartroute.dto.ApiDtos.BookingRequest;
import lk.smartroute.dto.ApiDtos.BookingResponse;
import lk.smartroute.dto.ApiDtos.ModifyBookingRequest;
import lk.smartroute.dto.ApiDtos.PageResponse;
import lk.smartroute.model.BookingStatus;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    PageResponse<BookingResponse> listAll(BookingStatus status, String keyword, Pageable pageable);

    BookingResponse create(String email, BookingRequest request);

    BookingResponse get(String email, Long bookingId, boolean staffAccess);

    BookingResponse cancel(String email, Long bookingId, boolean staffAccess);

    BookingResponse modifySeats(String email, Long bookingId, ModifyBookingRequest request);

    PageResponse<BookingResponse> history(String email, Pageable pageable);

    AvailableSeatsResponse availableSeats(Long scheduleId);
}
