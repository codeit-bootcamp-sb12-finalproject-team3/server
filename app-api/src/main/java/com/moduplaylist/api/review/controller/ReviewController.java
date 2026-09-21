package com.moduplaylist.api.review.controller;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.review.dto.ReviewResponse;
import com.moduplaylist.api.review.dto.ReviewSearchRequest;
import com.moduplaylist.api.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@GetMapping
	public ResponseEntity<CursorPageResponse<ReviewResponse>> findAll(
		@Valid @ModelAttribute ReviewSearchRequest request
	) {
		return ResponseEntity.ok(reviewService.findAll(request));
	}
}
