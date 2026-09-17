package com.moduplaylist.api.playlist.controller;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.api.playlist.service.PlaylistService;
import com.moduplaylist.core.playlist.exception.InvalidPlaylistSearchException;
import com.moduplaylist.core.playlist.repository.PlaylistSearch;
import com.moduplaylist.core.playlist.repository.PlaylistSearch.Sort;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

  private final PlaylistService playlistService;

  @PostMapping
  public ResponseEntity<PlaylistResponse> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody PlaylistCreateRequest request
  ) {
    UUID userId = userDetails.getUserId();

    PlaylistResponse response = playlistService.create(userId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

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
        playlistService.findAll(userId, search)
    );
  }

  @GetMapping("/{playlistId}")
  public ResponseEntity<PlaylistResponse> findById(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    return ResponseEntity.ok(playlistService.findById(userId, playlistId));
  }

  @PatchMapping("/{playlistId}")
  public ResponseEntity<PlaylistResponse> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId,
      @Valid @RequestBody PlaylistUpdateRequest request
  ) {
    UUID userId = userDetails.getUserId();

    return ResponseEntity.ok(playlistService.update(userId, playlistId, request));
  }

  @DeleteMapping("/{playlistId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    playlistService.delete(userId, playlistId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> subscribe(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    playlistService.subscribe(userId, playlistId);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> unsubscribe(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    playlistService.unsubscribe(userId, playlistId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> addContent(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId
  ) {
    UUID userId = userDetails.getUserId();

    playlistService.addContent(userId, playlistId, contentId);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> removeContent(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId
  ) {
    UUID userId = userDetails.getUserId();

    playlistService.removeContent(userId, playlistId, contentId);

    return ResponseEntity.noContent().build();
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
