package com.moduplaylist.api.playlist.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.api.playlist.service.PlaylistService;
import jakarta.validation.Valid;
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
}
