package com.moduplaylist.core.review.repository;

import com.moduplaylist.core.review.entity.Review;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

public interface ReviewQueryRepository {

    SearchResult search(SearchCondition condition);

    @Getter
    class SearchCondition {

        private final UUID contentId;
        private final UUID userId;
        private final Instant cursorCreatedAt;
        private final UUID cursorId;
        private final int limit;

        public SearchCondition(
                UUID contentId,
                UUID userId,
                Instant cursorCreatedAt,
                UUID cursorId,
                int limit) {
            if ((contentId == null) == (userId == null)) {
                throw new IllegalArgumentException(
                        "콘텐츠 ID와 사용자 ID 중 하나만 지정해야 합니다.");
            }
            if ((cursorCreatedAt == null) != (cursorId == null)) {
                throw new IllegalArgumentException(
                        "리뷰 작성 시각과 리뷰 ID는 함께 지정해야 합니다.");
            }
            if (limit < 1 || limit > 100) {
                throw new IllegalArgumentException(
                        "limit은 1부터 100 사이여야 합니다.");
            }

            this.contentId = contentId;
            this.userId = userId;
            this.cursorCreatedAt = cursorCreatedAt;
            this.cursorId = cursorId;
            this.limit = limit;
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
}
