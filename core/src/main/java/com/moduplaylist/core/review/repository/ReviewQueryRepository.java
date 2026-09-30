package com.moduplaylist.core.review.repository;

import com.moduplaylist.core.review.entity.Review;
import com.moduplaylist.core.review.exception.InvalidReviewSearchException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

public interface ReviewQueryRepository {

    SearchResult search(SearchCondition condition);

    @Getter
    class SearchCondition {

        private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
        private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
        private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

        private final UUID contentId;
        private final UUID userIdEqual;
        private final Instant cursorCreatedAt;
        private final BigDecimal cursorRating;
        private final UUID idAfter;
        private final int limit;
        private final Sort sort;
        private final Direction direction;

        public SearchCondition(
                UUID contentId,
                UUID userIdEqual,
                Instant cursorCreatedAt,
                BigDecimal cursorRating,
                UUID idAfter,
                int limit,
                Sort sort,
                Direction direction) {
            if (limit < 1 || limit > 100 || sort == null || direction == null) {
                throw new InvalidReviewSearchException();
            }
            validateCursor(cursorCreatedAt, cursorRating, idAfter, sort);

            this.contentId = contentId;
            this.userIdEqual = userIdEqual;
            this.cursorCreatedAt = cursorCreatedAt;
            this.cursorRating = cursorRating;
            this.idAfter = idAfter;
            this.limit = limit;
            this.sort = sort;
            this.direction = direction;
        }

        private static void validateCursor(
                Instant cursorCreatedAt,
                BigDecimal cursorRating,
                UUID idAfter,
                Sort sort) {
            boolean cursorAbsent = cursorCreatedAt == null
                    && cursorRating == null
                    && idAfter == null;
            if (cursorAbsent) {
                return;
            }
            if (idAfter == null) {
                throw new InvalidReviewSearchException();
            }
            if (sort == Sort.CREATED_AT) {
                if (cursorCreatedAt == null || cursorRating != null) {
                    throw new InvalidReviewSearchException();
                }
                return;
            }
            if (cursorRating == null || cursorCreatedAt != null
                    || cursorRating.compareTo(MIN_RATING) < 0
                    || cursorRating.compareTo(MAX_RATING) > 0
                    || cursorRating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
                throw new InvalidReviewSearchException();
            }
        }
    }

    @Getter
    class SearchResult {

        private final List<Review> reviews;
        private final long totalCount;
        private final boolean hasNext;

        public SearchResult(
                List<Review> reviews,
                long totalCount,
                boolean hasNext) {
            this.reviews = List.copyOf(reviews);
            this.totalCount = totalCount;
            this.hasNext = hasNext;
        }
    }

    enum Sort {
        CREATED_AT,
        RATING
    }

    enum Direction {
        ASCENDING,
        DESCENDING
    }
}
