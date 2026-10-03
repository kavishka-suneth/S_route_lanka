package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.service.ReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<ReviewResponse> create(Authentication authentication, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviews.create(authentication.getName(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN')")
    public PageResponse<ReviewResponse> list(@PageableDefault(sort = "createdAt") Pageable pageable) {
        return reviews.list(pageable);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PASSENGER')")
    public PageResponse<ReviewResponse> mine(Authentication authentication, @PageableDefault(sort = "createdAt") Pageable pageable) {
        return reviews.listMine(authentication.getName(), pageable);
    }
}
