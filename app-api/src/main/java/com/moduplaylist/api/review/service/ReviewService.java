package com.moduplaylist.api.review.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.review.dto.ReviewResponse;
import com.moduplaylist.api.review.dto.ReviewSearchRequest;

public interface ReviewService {

	CursorPageResponse<ReviewResponse> findAll(ReviewSearchRequest request);
}
