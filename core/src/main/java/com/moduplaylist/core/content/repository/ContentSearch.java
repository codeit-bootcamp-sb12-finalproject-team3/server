package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ContentSearch {
    private final ContentType type;
    private final UUID genreId;
    private final String sportType;
    private final UUID likedByUserId;
    private final Sort sort;
    private final LocalDate cursorReleaseDate;
    private final BigDecimal cursorRating;
    private final UUID cursorId;
    private final int limit;

    public ContentSearch(ContentType type, UUID genreId, String sportType,
            UUID likedByUserId, Sort sort, LocalDate cursorReleaseDate,
            BigDecimal cursorRating, UUID cursorId, int limit) {
        Objects.requireNonNull(sort, "정렬 기준은 필수입니다.");
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit은 1부터 100 사이여야 합니다.");
        }
        if ((cursorId == null) != (cursorRating == null)) {
            throw new IllegalArgumentException("평점과 콘텐츠 ID는 함께 지정해야 합니다.");
        }
        if (cursorId == null && cursorReleaseDate != null) {
            throw new IllegalArgumentException("개봉일과 콘텐츠 ID는 함께 지정해야 합니다.");
        }
        if (sort == Sort.RATING && cursorReleaseDate != null) {
            throw new IllegalArgumentException("평점순 커서에는 개봉일을 지정할 수 없습니다.");
        }
        this.type = type;
        this.genreId = genreId;
        this.sportType = sportType;
        this.likedByUserId = likedByUserId;
        this.sort = sort;
        this.cursorReleaseDate = cursorReleaseDate;
        this.cursorRating = cursorRating;
        this.cursorId = cursorId;
        this.limit = limit;
    }

    public enum Sort {
        RATING,
        LATEST
    }
}
