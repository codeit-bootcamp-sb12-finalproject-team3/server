package com.moduplaylist.core.review.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {
	private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
	private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
	private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "content_id", nullable = false, updatable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Content content;

	// 리뷰 대상 Content와 구분하기 위해 본문은 reviewText로 명명한다.
	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String reviewText;

	@Column(nullable = false, precision = 2, scale = 1)
	private BigDecimal rating;

	@Column(name = "is_spoiler", nullable = false)
	private boolean spoiler;

	private Review(
		User user,
		Content content,
		String reviewText,
		BigDecimal rating,
		boolean spoiler
	) {
		this.user = user;
		this.content = content;
		this.reviewText = reviewText;
		this.rating = rating;
		this.spoiler = spoiler;
	}

	public static Review create(
		User user,
		Content content,
		String reviewText,
		BigDecimal rating,
		boolean spoiler
	) {
		if (user == null || content == null) {
			throw new IllegalArgumentException(
				"리뷰 작성자와 대상 콘텐츠는 필수입니다."
			);
		}

		validateReviewText(reviewText);
		validateRating(rating);

		return new Review(
			user,
			content,
			reviewText,
			rating,
			spoiler
		);
	}

	public void update(String reviewText, BigDecimal rating, Boolean spoiler) {
		if (reviewText != null) validateReviewText(reviewText);
		if (rating != null) validateRating(rating);

		if (reviewText != null) this.reviewText = reviewText;
		if (rating != null) this.rating = rating;
		if (spoiler != null) this.spoiler = spoiler;
	}

	private static void validateReviewText(String reviewText) {
		if (reviewText == null || reviewText.isBlank()) {
			throw new IllegalArgumentException("리뷰 내용은 필수입니다.");
		}
	}

	private static void validateRating(BigDecimal rating) {
		if (rating == null || rating.compareTo(MIN_RATING) < 0 || rating.compareTo(MAX_RATING) > 0
			|| rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
			throw new IllegalArgumentException("평점은 0.5부터 5까지 0.5점 단위로 지정해야 합니다.");
		}
	}
}
