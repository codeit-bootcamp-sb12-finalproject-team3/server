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
            ContentType type,
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
            throw new InvalidContentSearchException();
        }
        if (sportType != null && !sportType.isBlank()
                && type != null && type != ContentType.SPORT) {
            throw new InvalidContentSearchException();
        }

        this.type = type;
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
                throw new InvalidContentSearchException();
            }
            requirePair(cursorLikedAt, cursorId);
            if (cursorCreatedAt != null || cursorRating != null) {
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
            requirePair(cursorCreatedAt, cursorId);
            if (cursorRating != null) {
                throw new InvalidContentSearchException();
            }
            return;
        }

        requirePair(cursorRating, cursorId);
        if (cursorCreatedAt != null) {
            throw new InvalidContentSearchException();
        }
    }

    private static void requirePair(Object value, UUID cursorId) {
        if ((value == null) != (cursorId == null)) {
            throw new InvalidContentSearchException();
        }
    }

    public enum Sort {
        LATEST,
        RATING
    }
}
