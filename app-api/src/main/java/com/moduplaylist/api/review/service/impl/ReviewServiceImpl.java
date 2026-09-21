package com.moduplaylist.api.review.service.impl;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.review.dto.ReviewCreateRequest;
import com.moduplaylist.api.review.dto.ReviewResponse;
import com.moduplaylist.api.review.dto.ReviewSearchRequest;
import com.moduplaylist.api.review.dto.ReviewSort;
import com.moduplaylist.api.review.dto.ReviewUpdateRequest;
import com.moduplaylist.api.review.service.ReviewService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.review.entity.Review;
import com.moduplaylist.core.review.exception.ContentNotReviewableException;
import com.moduplaylist.core.review.exception.InvalidReviewSearchException;
import com.moduplaylist.core.review.exception.ReviewAccessDeniedException;
import com.moduplaylist.core.review.exception.ReviewAlreadyExistsException;
import com.moduplaylist.core.review.exception.ReviewNotFoundException;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.Direction;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.SearchCondition;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.SearchResult;
import com.moduplaylist.core.review.repository.ReviewQueryRepository.Sort;
import com.moduplaylist.core.review.repository.ReviewRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private static final String UNIQUE_USER_CONTENT_CONSTRAINT = "uq_reviews_user_content";
	private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
	private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
	private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

	private final ReviewRepository reviewRepository;
	private final ContentRepository contentRepository;
	private final UserRepository userRepository;

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

	@Override
	@Transactional
	public ReviewResponse create(UUID userId, ReviewCreateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException(userId));
		UUID contentId = request.getContentId();
		Content content = contentRepository.findByIdForUpdate(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
		if (content.isHidden()) {
			throw new ContentNotFoundException(contentId);
		}
		if (!content.isReviewable()) {
			throw new ContentNotReviewableException(contentId);
		}
		if (reviewRepository.existsByUser_IdAndContent_Id(userId, contentId)) {
			throw new ReviewAlreadyExistsException(userId, contentId);
		}

		Review review = Review.create(
			user,
			content,
			request.getText(),
			request.getRating(),
			false
		);
		Review savedReview;
		try {
			savedReview = reviewRepository.saveAndFlush(review);
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateReviewConstraint(exception)) {
				throw new ReviewAlreadyExistsException(userId, contentId, exception);
			}
			throw exception;
		}
		ReviewRepository.ReviewStatisticsProjection statistics =
			reviewRepository.calculateStatistics(contentId);
		content.updateReviewStatistics(
			statistics.getAverageRating(),
			statistics.getReviewCount()
		);
		return ReviewResponse.from(savedReview);
	}

	@Override
	@Transactional
	public ReviewResponse update(
		UUID userId,
		UUID reviewId,
		ReviewUpdateRequest request
	) {
		ReviewRepository.ReviewAccessProjection access = reviewRepository.findAccessById(reviewId)
			.orElseThrow(() -> new ReviewNotFoundException(reviewId));
		if (!access.getUserId().equals(userId)) {
			throw new ReviewAccessDeniedException(reviewId, userId);
		}

		UUID contentId = access.getContentId();
		Content content = contentRepository.findByIdForUpdate(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
		Review review = reviewRepository.findByIdForUpdate(reviewId)
			.orElseThrow(() -> new ReviewNotFoundException(reviewId));
		BigDecimal previousRating = review.getRating();

		review.update(request.getText(), request.getRating(), null);
		boolean ratingChanged = request.getRating() != null
			&& previousRating.compareTo(review.getRating()) != 0;
		if (ratingChanged) {
			reviewRepository.flush();
			ReviewRepository.ReviewStatisticsProjection statistics =
				reviewRepository.calculateStatistics(contentId);
			content.updateReviewStatistics(
				statistics.getAverageRating(),
				statistics.getReviewCount()
			);
		}
		return ReviewResponse.from(review);
	}

	private boolean isDuplicateReviewConstraint(Throwable exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof ConstraintViolationException violation
				&& UNIQUE_USER_CONTENT_CONSTRAINT.equalsIgnoreCase(
					unqualifiedConstraintName(violation.getConstraintName()))) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}

	private String unqualifiedConstraintName(String constraintName) {
		if (constraintName == null) {
			return null;
		}
		int qualifierSeparator = constraintName.lastIndexOf('.');
		return qualifierSeparator < 0
			? constraintName
			: constraintName.substring(qualifierSeparator + 1);
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
