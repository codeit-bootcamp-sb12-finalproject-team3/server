package com.moduplaylist.api.review.service.impl;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.review.dto.ReviewResponse;
import com.moduplaylist.api.review.dto.ReviewSearchRequest;
import com.moduplaylist.api.review.dto.ReviewSort;
import com.moduplaylist.api.review.service.ReviewService;
import com.moduplaylist.core.review.entity.Review;
import com.moduplaylist.core.review.exception.InvalidReviewSearchException;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.Direction;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.SearchCondition;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.SearchResult;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.Sort;
import com.moduplaylist.core.review.repository.ReviewRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
	private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
	private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

	private final ReviewRepository reviewRepository;

	@Override
	@Transactional(readOnly = true)
	public CursorPageResponse<ReviewResponse> findAll(ReviewSearchRequest request) {
		ParsedCursor cursor = parseCursor(request.getCursor(), request.getSortBy());
		SearchCondition condition = new SearchCondition(
			request.getContentId(),
			cursor.createdAt(),
			cursor.rating(),
			request.getIdAfter(),
			request.getLimit(),
			toRepositorySort(request.getSortBy()),
			toRepositoryDirection(request.getSortDirection())
		);

		SearchResult result = reviewRepository.search(condition);
		List<Review> reviews = result.getReviews();
		List<ReviewResponse> data = reviews.stream()
			.map(ReviewResponse::from)
			.toList();
		Review lastReview = reviews.isEmpty() ? null : reviews.get(reviews.size() - 1);

		return CursorPageResponse.<ReviewResponse>builder()
			.data(data)
			.nextCursor(result.isHasNext() && lastReview != null
				? cursorValue(lastReview, request.getSortBy())
				: null)
			.nextIdAfter(result.isHasNext() && lastReview != null
				? lastReview.getId()
				: null)
			.hasNext(result.isHasNext())
			.totalCount(result.getTotalCount())
			.sortBy(request.getSortBy().getValue())
			.sortDirection(request.getSortDirection())
			.build();
	}

	private ParsedCursor parseCursor(String value, ReviewSort sort) {
		if (value == null || value.isBlank()) {
			return ParsedCursor.empty();
		}
		try {
			if (sort == ReviewSort.CREATED_AT) {
				return ParsedCursor.createdAt(Instant.parse(value));
			}
			BigDecimal rating = new BigDecimal(value);
			if (rating.compareTo(MIN_RATING) < 0
				|| rating.compareTo(MAX_RATING) > 0
				|| rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
				throw new InvalidReviewSearchException();
			}
			return ParsedCursor.rating(rating);
		} catch (DateTimeParseException | NumberFormatException exception) {
			throw new InvalidReviewSearchException();
		}
	}

	private String cursorValue(Review review, ReviewSort sort) {
		return sort == ReviewSort.CREATED_AT
			? review.getCreatedAt().toString()
			: review.getRating().toPlainString();
	}

	private Sort toRepositorySort(ReviewSort sort) {
		return sort == ReviewSort.CREATED_AT ? Sort.CREATED_AT : Sort.RATING;
	}

	private Direction toRepositoryDirection(SortDirection direction) {
		return direction == SortDirection.ASCENDING
			? Direction.ASCENDING
			: Direction.DESCENDING;
	}

	private record ParsedCursor(Instant createdAt, BigDecimal rating) {

		private static ParsedCursor empty() {
			return new ParsedCursor(null, null);
		}

		private static ParsedCursor createdAt(Instant value) {
			return new ParsedCursor(value, null);
		}

		private static ParsedCursor rating(BigDecimal value) {
			return new ParsedCursor(null, value);
		}
	}
}
