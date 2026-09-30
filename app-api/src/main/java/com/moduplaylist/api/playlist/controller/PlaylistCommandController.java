package com.moduplaylist.api.playlist.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.playlist.dto.AiPlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.api.playlist.service.AiPlaylistGenerationService;
import com.moduplaylist.api.playlist.service.PlaylistCommandService;
import com.moduplaylist.api.playlist.service.PlaylistContentService;
import com.moduplaylist.api.playlist.service.PlaylistSubscriptionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistCommandController {

  private final PlaylistCommandService playlistCommandService;
  private final PlaylistSubscriptionService playlistSubscriptionService;
  private final PlaylistContentService playlistContentService;
  private final AiPlaylistGenerationService aiPlaylistGenerationService;

  @PostMapping
  public ResponseEntity<PlaylistResponse> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody PlaylistCreateRequest request
  ) {
    UUID userId = userDetails.getUserId();

    PlaylistResponse response =
        playlistCommandService.create(userId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/ai")
  public ResponseEntity<PlaylistResponse> createAiPlaylist(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody AiPlaylistCreateRequest request
  ) {
    UUID userId = userDetails.getUserId();

    PlaylistResponse response = aiPlaylistGenerationService.create(userId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{playlistId}")
  public ResponseEntity<PlaylistResponse> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId,
      @Valid @RequestBody PlaylistUpdateRequest request
  ) {
    UUID userId = userDetails.getUserId();

    return ResponseEntity.ok(
        playlistCommandService.update(userId, playlistId, request)
    );
  }

  @DeleteMapping("/{playlistId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    playlistCommandService.delete(userId, playlistId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> subscribe(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    playlistSubscriptionService.subscribe(userId, playlistId);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> unsubscribe(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId
  ) {
    UUID userId = userDetails.getUserId();

    playlistSubscriptionService.unsubscribe(userId, playlistId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> addContent(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId
  ) {
    UUID userId = userDetails.getUserId();

    playlistContentService.addContent(userId, playlistId, contentId);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> removeContent(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId
  ) {
    UUID userId = userDetails.getUserId();

    playlistContentService.removeContent(userId, playlistId, contentId);

    return ResponseEntity.noContent().build();
  }
}
