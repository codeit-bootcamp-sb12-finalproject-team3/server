package com.moduplaylist.api.playlist.controller;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.playlist.service.PlaylistQueryService;
import com.moduplaylist.core.playlist.exception.InvalidPlaylistSearchException;
import com.moduplaylist.core.playlist.repository.PlaylistSearch;
import com.moduplaylist.core.playlist.repository.PlaylistSearch.Sort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistQueryController {

  private final PlaylistQueryService playlistQueryService;

  @GetMapping
  public ResponseEntity<CursorPageResponse<PlaylistSummaryResponse>> findAll(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) UUID ownerIdEqual,
      @RequestParam(required = false) UUID subscriberIdEqual,
      @RequestParam(required = false) UUID contentIdEqual,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam String sortBy,
      @RequestParam String sortDirection
  ) {
    UUID userId = userDetails.getUserId();

    PlaylistSearch.Sort sort = parseSort(sortBy);
    PlaylistSearch.Direction direction = parseDirection(sortDirection);

    Instant cursorCreatedAt = null;
    BigDecimal cursorWeeklyPopularityScore = null;

    if (cursor != null) {
      switch (sort) {
        case CREATED_AT ->
            cursorCreatedAt = parseCreatedAtCursor(cursor);

        case WEEKLY_POPULARITY_SCORE ->
            cursorWeeklyPopularityScore = parsePopularityCursor(cursor);
      }
    }

    PlaylistSearch search = new PlaylistSearch(
        ownerIdEqual,
        subscriberIdEqual,
        contentIdEqual,
        cursorCreatedAt,
        cursorWeeklyPopularityScore,
        idAfter,
        limit,
        sort,
        direction
    );

    return ResponseEntity.ok(
        playlistQueryService.findAll(userId, search)
    );
  }

  @GetMapping("/{playlistId}")
  public ResponseEntity<PlaylistResponse> findById(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    return ResponseEntity.ok(
        playlistQueryService.findById(userId, playlistId)
    );
  }

  private PlaylistSearch.Sort parseSort(String sortBy) {
    return switch (sortBy) {
      case "createdAt" ->
          Sort.CREATED_AT;

      case "weeklyPopularityScore" ->
          Sort.WEEKLY_POPULARITY_SCORE;

      default ->
          throw new InvalidPlaylistSearchException(
              "지원하지 않는 정렬 기준입니다."
          );
    };
  }

  private PlaylistSearch.Direction parseDirection(String sortDirection) {
    try {
      return PlaylistSearch.Direction.valueOf(sortDirection);
    } catch (IllegalArgumentException e) {
      throw new InvalidPlaylistSearchException(
          "지원하지 않는 정렬 방향입니다."
      );
    }
  }

  private Instant parseCreatedAtCursor(String cursor) {
    try {
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new InvalidPlaylistSearchException(
          "createdAt cursor 형식이 올바르지 않습니다."
      );
    }
  }

  private BigDecimal parsePopularityCursor(String cursor) {
    try {
      return new BigDecimal(cursor);
    } catch (NumberFormatException e) {
      throw new InvalidPlaylistSearchException(
          "weeklyPopularityScore cursor 형식이 올바르지 않습니다."
      );
    }
  }
}
