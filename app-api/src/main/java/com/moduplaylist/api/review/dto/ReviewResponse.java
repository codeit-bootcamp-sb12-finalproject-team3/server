package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moduplaylist.core.review.entity.Review;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {

	private UUID id;
	private UUID userId;
	private UUID contentId;
	private String content;
	private BigDecimal rating;

	@JsonProperty("isSpoiler")
	private boolean isSpoiler;

	private Instant createdAt;
	private Instant updatedAt;

	public static ReviewResponse from(Review review) {
		return ReviewResponse.builder()
			.id(review.getId())
			.userId(review.getUser().getId())
			.contentId(review.getContent().getId())
			.content(review.getReviewText())
			.rating(review.getRating())
			.isSpoiler(review.isSpoiler())
			.createdAt(review.getCreatedAt())
			.updatedAt(review.getUpdatedAt())
			.build();
	}
}
