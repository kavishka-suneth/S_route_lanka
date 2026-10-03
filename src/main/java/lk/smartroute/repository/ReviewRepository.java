package lk.smartroute.repository;

import lk.smartroute.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    Page<Review> findByPassengerId(Long passengerId, Pageable pageable);

    Page<Review> findByPassengerUserEmailIgnoreCase(String email, Pageable pageable);

    List<Review> findByRating(int rating);

    @Query("select coalesce(avg(r.rating), 0) from Review r")
    double averageRating();

    @Query("select r.rating as rating, count(r) as reviewCount from Review r group by r.rating order by r.rating")
    List<ReviewRatingCount> countReviewsGroupedByRating();

    List<Review> findTop10ByOrderByReviewDateDesc();

    interface ReviewRatingCount {
        int getRating();
        long getReviewCount();
    }
}
