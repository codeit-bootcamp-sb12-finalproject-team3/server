package com.moduplaylist.core.content.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ContentSearch {

    private final String sportType;
    private final UUID likedByUserId;
    private final List<UUID> matchedContentIds;
    private final Sort sort;
    private final Instant cursorCreatedAt;
    private final Instant cursorLikedAt;
    private final BigDecimal cursorRating;
    private final UUID cursorId;
    private final int limit;

    public ContentSearch(
            String sportType,
            UUID likedByUserId,
            Collection<UUID> matchedContentIds,
            Sort sort,
            Instant cursorCreatedAt,
            Instant cursorLikedAt,
            BigDecimal cursorRating,
            UUID cursorId,
            int limit) {
        validateSortAndCursors(
                likedByUserId,
                sort,
                cursorCreatedAt,
                cursorLikedAt,
                cursorRating,
                cursorId);
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit은 1부터 100 사이여야 합니다.");
        }

        this.sportType = sportType;
        this.likedByUserId = likedByUserId;
        this.matchedContentIds = matchedContentIds == null
                ? null
                : List.copyOf(matchedContentIds);
        this.sort = sort;
        this.cursorCreatedAt = cursorCreatedAt;
        this.cursorLikedAt = cursorLikedAt;
        this.cursorRating = cursorRating;
        this.cursorId = cursorId;
        this.limit = limit;
    }

    public boolean isLikedContentsSearch() {
        return likedByUserId != null;
    }

    public boolean hasContentIdFilter() {
        return matchedContentIds != null;
    }

    private static void validateSortAndCursors(
            UUID likedByUserId,
            Sort sort,
            Instant cursorCreatedAt,
            Instant cursorLikedAt,
            BigDecimal cursorRating,
            UUID cursorId) {
        if (likedByUserId != null) {
            if (sort != null) {
                throw new IllegalArgumentException(
                        "좋아요 콘텐츠 조회에는 일반 정렬 기준을 지정할 수 없습니다.");
            }
            requirePair(cursorLikedAt, cursorId, "좋아요 등록 시각과 콘텐츠 ID");
            if (cursorCreatedAt != null || cursorRating != null) {
                throw new IllegalArgumentException(
                        "좋아요 콘텐츠 조회에는 일반 목록 커서를 지정할 수 없습니다.");
            }
            return;
        }

        Objects.requireNonNull(sort, "일반 콘텐츠 조회의 정렬 기준은 필수입니다.");
        if (cursorLikedAt != null) {
            throw new IllegalArgumentException(
                    "일반 콘텐츠 조회에는 좋아요 목록 커서를 지정할 수 없습니다.");
        }
        if (sort == Sort.LATEST) {
            requirePair(cursorCreatedAt, cursorId, "콘텐츠 등록 시각과 콘텐츠 ID");
            if (cursorRating != null) {
                throw new IllegalArgumentException(
                        "최신순 조회에는 평점 커서를 지정할 수 없습니다.");
            }
            return;
        }

        requirePair(cursorRating, cursorId, "평점과 콘텐츠 ID");
        if (cursorCreatedAt != null) {
            throw new IllegalArgumentException(
                    "평점순 조회에는 콘텐츠 등록 시각 커서를 지정할 수 없습니다.");
        }
    }

    private static void requirePair(Object value, UUID cursorId, String fields) {
        if ((value == null) != (cursorId == null)) {
            throw new IllegalArgumentException(fields + "는 함께 지정해야 합니다.");
        }
    }

    public enum Sort {
        LATEST,
        RATING
    }
}
