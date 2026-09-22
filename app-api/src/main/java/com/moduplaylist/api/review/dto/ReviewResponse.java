package com.moduplaylist.api.review.dto;

import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.review.entity.Review;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {

	private UUID id;
	private UUID contentId;
	private UserSummary author;
	private String text;
	private BigDecimal rating;

	public static ReviewResponse from(Review review) {
		return ReviewResponse.builder()
			.id(review.getId())
			.contentId(review.getContent().getId())
			.author(UserSummary.from(review.getUser()))
			.text(review.getReviewText())
			.rating(review.getRating())
			.build();
	}
}
