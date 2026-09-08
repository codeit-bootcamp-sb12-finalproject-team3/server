package com.moduplaylist.api.review.service.impl;

import com.moduplaylist.api.review.dto.*;
import com.moduplaylist.api.review.service.*;
import com.moduplaylist.api.global.dto.*;
import com.moduplaylist.core.common.exception.*;
import com.moduplaylist.core.content.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.review.Review;
import com.moduplaylist.core.review.repository.*;
import com.moduplaylist.core.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviews;
    private final ReviewQueryRepository queries;
    private final ContentRepository contents;
    private final UserRepository users;
    private final ReviewActor actor;

    @Override
    public ReviewResponse findById(UUID id, boolean includeSpoilers) {
        return response(reviews.findById(id).orElseThrow(() -> error(ErrorCode.REVIEW_NOT_FOUND, id)), includeSpoilers);
    }

    @Override
    public CursorPageResponse<ReviewResponse> findAll(ReviewListRequest request) {
        if ((request.getCursor() == null) != (request.getIdAfter() == null)) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        Instant cursor = null;
        if (request.getCursor() != null) {
            try { cursor = Instant.parse(request.getCursor()); }
            catch (RuntimeException e) { throw new BaseException(ErrorCode.INVALID_REQUEST); }
        }
        var found = queries.search(request.getContentIdEqual(), request.getUserIdEqual(), cursor,
                request.getIdAfter(), request.getSortDirection() == SortDirection.ASCENDING, request.getLimit());
        boolean hasNext = found.data().size() > request.getLimit();
        List<Review> page = found.data().stream().limit(request.getLimit()).toList();
        Review last = hasNext ? page.get(page.size() - 1) : null;
        return CursorPageResponse.<ReviewResponse>builder()
                .data(page.stream().map(r -> response(r, request.isIncludeSpoilers())).toList())
                .hasNext(hasNext).nextCursor(last == null ? null : last.getCreatedAt().toString())
                .nextIdAfter(last == null ? null : last.getId()).totalCount(found.totalCount())
                .sortBy(request.getSortBy()).sortDirection(request.getSortDirection()).build();
    }

    @Override
    @Transactional
    public ReviewResponse create(ReviewCreateRequest request) {
        UUID userId = actor.userId();
        Content content = lockContent(request.getContentId());
        var user = users.findById(userId).orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        if (reviews.existsByUser_IdAndContent_Id(userId, content.getId())) {
            throw new BaseException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        Review review = Review.builder().user(user).content(content).reviewText(request.getReviewText())
                .rating(request.getRating()).spoiler(request.isSpoiler()).build();
        try {
            reviews.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            // 다른 저장 경로와 경합하더라도 리뷰 중복 제약만 409로 변환한다.
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation.getConstraintName().toLowerCase(Locale.ROOT).contains("uq_reviews_user_content")) {
                    throw new BaseException(ErrorCode.REVIEW_ALREADY_EXISTS, e);
                }
            }
            throw e;
        }
        updateStatistics(content);
        return response(review, true);
    }

    @Override
    @Transactional
    public ReviewResponse update(UUID id, ReviewUpdateRequest request) {
        UUID userId = actor.userId();
        Content content = lockReviewContent(id);
        Review review = ownedReview(id, userId);
        review.update(request.getReviewText(), request.getRating(), request.getSpoiler());
        reviews.flush();
        updateStatistics(content);
        return response(review, true);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        UUID userId = actor.userId();
        Content content = lockReviewContent(id);
        Review review = ownedReview(id, userId);
        reviews.delete(review);
        reviews.flush();
        updateStatistics(content);
    }

    private Content lockReviewContent(UUID reviewId) {
        UUID contentId = queries.contentId(reviewId)
                .orElseThrow(() -> error(ErrorCode.REVIEW_NOT_FOUND, reviewId));
        return contents.findByIdForUpdate(contentId)
                .orElseThrow(() -> error(ErrorCode.REVIEW_NOT_FOUND, reviewId));
    }

    private Content lockContent(UUID id) {
        return contents.findByIdForUpdate(id).orElseThrow(() -> {
            BaseException exception = new BaseException(ErrorCode.CONTENT_NOT_FOUND);
            exception.addDetail("contentId", id);
            return exception;
        });
    }

    private Review ownedReview(UUID id, UUID userId) {
        Review review = reviews.findByIdForUpdate(id).orElseThrow(() -> error(ErrorCode.REVIEW_NOT_FOUND, id));
        if (!review.getUser().getId().equals(userId)) throw error(ErrorCode.REVIEW_ACCESS_DENIED, id);
        return review;
    }

    private void updateStatistics(Content content) {
        List<BigDecimal> ratings = queries.ratingsForUpdate(content.getId());
        BigDecimal sum = ratings.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = ratings.isEmpty() ? new BigDecimal("0.00")
                : sum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        content.updateReviewStatistics(average, ratings.size());
        contents.flush();
    }

    private BaseException error(ErrorCode code, UUID reviewId) {
        BaseException exception = new BaseException(code);
        exception.addDetail("reviewId", reviewId);
        return exception;
    }

    private ReviewResponse response(Review review, boolean includeSpoilers) {
        return ReviewResponse.builder().id(review.getId()).userId(review.getUser().getId())
                .contentId(review.getContent().getId())
                .reviewText(review.isSpoiler() && !includeSpoilers ? null : review.getReviewText())
                .rating(review.getRating()).spoiler(review.isSpoiler())
                .createdAt(review.getCreatedAt()).updatedAt(review.getUpdatedAt()).build();
    }
}
