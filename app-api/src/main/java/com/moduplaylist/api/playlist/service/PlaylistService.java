package com.moduplaylist.api.playlist.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.core.playlist.repository.PlaylistSearch;
import java.util.UUID;

public interface PlaylistService {

  PlaylistResponse create(UUID userId, PlaylistCreateRequest request);

  PlaylistResponse findById(UUID userId, UUID playlistId);

  CursorPageResponse<PlaylistSummaryResponse> findAll(
      UUID userId,
      PlaylistSearch search
  );

  PlaylistResponse update(
      UUID userId,
      UUID playlistId,
      PlaylistUpdateRequest request);

  void delete(UUID userId, UUID playlistId);
}
