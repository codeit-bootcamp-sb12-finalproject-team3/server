package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ContentSearch {

    private final ContentType type;
    private final UUID genreId;
    private final String sportType;
    private final UUID likedByUserId;
    private final List<UUID> matchedContentIds;
    private final Sort sort;
    private final Instant cursorCreatedAt;
    private final Instant cursorLikedAt;
    private final BigDecimal cursorRating;
    private final Long cursorReviewCount;
    private final UUID idAfter;
    private final int limit;

    public ContentSearch(
            ContentType type,
            UUID genreId,
            String sportType,
            UUID likedByUserId,
            Collection<UUID> matchedContentIds,
            Sort sort,
            Instant cursorCreatedAt,
            Instant cursorLikedAt,
            BigDecimal cursorRating,
            Long cursorReviewCount,
            UUID idAfter,
            int limit) {
        validateSortAndCursors(
                likedByUserId,
                sort,
                cursorCreatedAt,
                cursorLikedAt,
                cursorRating,
                cursorReviewCount,
                idAfter);
        if (limit < 1 || limit > 100) {
            throw new InvalidContentSearchException();
        }
        if (sportType != null && !sportType.isBlank()
                && type != ContentType.SPORT) {
            throw new InvalidContentSearchException();
        }
        if (genreId != null
                && type != ContentType.MOVIE
                && type != ContentType.TV_SEASON) {
            throw new InvalidContentSearchException();
        }
        if (genreId != null && sportType != null && !sportType.isBlank()) {
            throw new InvalidContentSearchException();
        }
        if (matchedContentIds != null
                && (genreId != null
                || sportType != null && !sportType.isBlank())) {
            throw new InvalidContentSearchException();
        }
        if (likedByUserId == null
                && matchedContentIds != null
                && matchedContentIds.size() > 100) {
            throw new InvalidContentSearchException();
        }

        this.type = type;
        this.genreId = genreId;
        this.sportType = sportType;
        this.likedByUserId = likedByUserId;
        this.matchedContentIds = matchedContentIds == null
                ? null
                : List.copyOf(matchedContentIds);
        this.sort = sort;
        this.cursorCreatedAt = cursorCreatedAt;
        this.cursorLikedAt = cursorLikedAt;
        this.cursorRating = cursorRating;
        this.cursorReviewCount = cursorReviewCount;
        this.idAfter = idAfter;
        this.limit = limit;
    }

    public boolean hasContentIdFilter() {
        return matchedContentIds != null;
    }

    public boolean isLikedContentsSearch() {
        return likedByUserId != null;
    }

    private static void validateSortAndCursors(
            UUID likedByUserId,
            Sort sort,
            Instant cursorCreatedAt,
            Instant cursorLikedAt,
            BigDecimal cursorRating,
            Long cursorReviewCount,
            UUID idAfter) {
        if (likedByUserId != null) {
            if (sort != null) {
                throw new InvalidContentSearchException();
            }
            requirePair(cursorLikedAt, idAfter);
            if (cursorCreatedAt != null
                    || cursorRating != null
                    || cursorReviewCount != null) {
                throw new InvalidContentSearchException();
            }
            return;
        }

        if (sort == null) {
            throw new InvalidContentSearchException();
        }
        if (cursorLikedAt != null) {
            throw new InvalidContentSearchException();
        }
        if (sort == Sort.LATEST) {
            requirePair(cursorCreatedAt, idAfter);
            if (cursorRating != null || cursorReviewCount != null) {
                throw new InvalidContentSearchException();
            }
            return;
        }

        requireRatingCursor(cursorRating, cursorReviewCount, idAfter);
        if (cursorCreatedAt != null) {
            throw new InvalidContentSearchException();
        }
    }

    private static void requirePair(Object value, UUID idAfter) {
        if ((value == null) != (idAfter == null)) {
            throw new InvalidContentSearchException();
        }
    }

    private static void requireRatingCursor(
            BigDecimal cursorRating,
            Long cursorReviewCount,
            UUID idAfter) {
        boolean absent = cursorRating == null
                && cursorReviewCount == null
                && idAfter == null;
        boolean complete = cursorRating != null
                && cursorReviewCount != null
                && cursorReviewCount >= 0
                && idAfter != null;
        if (!absent && !complete) {
            throw new InvalidContentSearchException();
        }
    }

    public enum Sort {
        LATEST,
        RATING
    }
}
