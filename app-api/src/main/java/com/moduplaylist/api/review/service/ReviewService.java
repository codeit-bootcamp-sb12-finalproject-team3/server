package com.moduplaylist.api.review.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.review.dto.ReviewCreateRequest;
import com.moduplaylist.api.review.dto.ReviewResponse;
import com.moduplaylist.api.review.dto.ReviewSearchRequest;
import com.moduplaylist.api.review.dto.ReviewUpdateRequest;
import java.util.UUID;

public interface ReviewService {

	CursorPageResponse<ReviewResponse> findAll(ReviewSearchRequest request);

	ReviewResponse create(UUID userId, ReviewCreateRequest request);

	ReviewResponse update(UUID userId, UUID reviewId, ReviewUpdateRequest request);
}
