package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponse create(String email, ReviewRequest request);

    PageResponse<ReviewResponse> list(Pageable pageable);

    PageResponse<ReviewResponse> listMine(String email, Pageable pageable);
}
