package com.moduplaylist.api.review.controller;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.review.dto.ReviewCreateRequest;
import com.moduplaylist.api.review.dto.ReviewResponse;
import com.moduplaylist.api.review.dto.ReviewSearchRequest;
import com.moduplaylist.api.review.dto.ReviewUpdateRequest;
import com.moduplaylist.api.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PostMapping
	public ResponseEntity<ReviewResponse> create(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody ReviewCreateRequest request
	) {
		ReviewResponse response = reviewService.create(userDetails.getUserId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PatchMapping("/{reviewId}")
	public ResponseEntity<ReviewResponse> update(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID reviewId,
		@Valid @RequestBody ReviewUpdateRequest request
	) {
		return ResponseEntity.ok(
			reviewService.update(userDetails.getUserId(), reviewId, request)
		);
	}
}
