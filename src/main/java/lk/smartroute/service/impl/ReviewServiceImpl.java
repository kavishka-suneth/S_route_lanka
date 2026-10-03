package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ForbiddenActionException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.Booking;
import lk.smartroute.model.BookingStatus;
import lk.smartroute.model.Review;
import lk.smartroute.repository.BookingRepository;
import lk.smartroute.repository.ReviewRepository;
import lk.smartroute.service.ReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;

    public ReviewServiceImpl(ReviewRepository reviews, BookingRepository bookings) {
        this.reviews = reviews;
        this.bookings = bookings;
    }

    @Override
    public ReviewResponse create(String email, ReviewRequest request) {
        Booking booking = bookings.findById(request.bookingId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getPassenger().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenActionException("Booking does not belong to user");
        }
        if (!java.util.List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED).contains(booking.getBookingStatus())) {
            throw new IllegalStateException("Only confirmed or completed bookings can be reviewed");
        }
        if (reviews.existsByBookingId(booking.getId())) {
            throw new ConflictException("A review already exists for this booking");
        }
        Review review = new Review();
        review.setBooking(booking);
        review.setPassenger(booking.getPassenger());
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setReviewDate(java.time.LocalDateTime.now());
        Review saved = reviews.save(review);
        return DtoMapper.review(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> list(Pageable pageable) {
        return PageResponse.from(reviews.findAll(pageable).map(DtoMapper::review));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listMine(String email, Pageable pageable) {
        return PageResponse.from(reviews.findByPassengerUserEmailIgnoreCase(email, pageable).map(DtoMapper::review));
    }
}
