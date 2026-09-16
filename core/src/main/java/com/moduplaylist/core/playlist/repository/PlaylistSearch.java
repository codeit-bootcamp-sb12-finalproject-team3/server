package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.exception.InvalidPlaylistSearchException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public class PlaylistSearch {

  private final UUID ownerIdEqual;
  private final UUID subscriberIdEqual;
  private final UUID contentIdEqual;

  private final Instant cursorCreatedAt;
  private final BigDecimal cursorWeeklyPopularityScore;
  private final UUID cursorId;

  private final int limit;
  private final Sort sort;
  private final Direction direction;

  public PlaylistSearch(
      UUID ownerIdEqual,
      UUID subscriberIdEqual,
      UUID contentIdEqual,
      Instant cursorCreatedAt,
      BigDecimal cursorWeeklyPopularityScore,
      UUID cursorId,
      int limit,
      Sort sort,
      Direction direction
  ) {

    if (limit < 1 || limit > 100) {
      throw new InvalidPlaylistSearchException("limit은 1부터 100 사이여야 합니다.");
    }

    if (sort == null) {
      throw new InvalidPlaylistSearchException("정렬 기준은 필수입니다.");
    }

    if (direction == null) {
      throw new InvalidPlaylistSearchException("정렬 방향은 필수입니다.");
    }

    validateCursor(
        cursorCreatedAt,
        cursorWeeklyPopularityScore,
        cursorId,
        sort
    );

    this.ownerIdEqual = ownerIdEqual;
    this.subscriberIdEqual = subscriberIdEqual;
    this.contentIdEqual = contentIdEqual;
    this.cursorCreatedAt = cursorCreatedAt;
    this.cursorWeeklyPopularityScore = cursorWeeklyPopularityScore;
    this.cursorId = cursorId;
    this.limit = limit;
    this.sort = sort;
    this.direction = direction;
  }

  private static void validateCursor(
      Instant cursorCreatedAt,
      BigDecimal cursorWeeklyPopularityScore,
      UUID cursorId,
      Sort sort
  ) {
    boolean hasCreatedAtCursor = cursorCreatedAt != null;
    boolean hasPopularityCursor = cursorWeeklyPopularityScore != null;
    boolean hasIdCursor = cursorId != null;

    if (!hasCreatedAtCursor && !hasPopularityCursor && !hasIdCursor) {
      return;
    }

    if (!hasIdCursor) {
      throw new InvalidPlaylistSearchException(
          "cursor와 idAfter는 함께 지정해야 합니다."
      );
    }

    if (sort == Sort.CREATED_AT) {
      if (!hasCreatedAtCursor || hasPopularityCursor) {
        throw new InvalidPlaylistSearchException(
            "최신순 정렬에는 생성일 커서와 idAfter를 함께 지정해야 합니다."
        );
      }
      return;
    }

    if (sort == Sort.WEEKLY_POPULARITY_SCORE) {
      if (!hasPopularityCursor || hasCreatedAtCursor) {
        throw new InvalidPlaylistSearchException(
            "인기순 정렬에는 주간 인기 점수 커서와 idAfter를 함께 지정해야 합니다."
        );
      }
    }
  }

  public enum Sort {
    CREATED_AT,
    WEEKLY_POPULARITY_SCORE
  }

  public enum Direction {
    ASCENDING,
    DESCENDING
  }
}
