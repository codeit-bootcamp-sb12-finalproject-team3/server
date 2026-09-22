package com.moduplaylist.api.playlist.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.core.playlist.repository.PlaylistSearch;
import java.util.UUID;

public interface PlaylistQueryService {

  PlaylistResponse findById(UUID userId, UUID playlistId);

  CursorPageResponse<PlaylistSummaryResponse> findAll(
      UUID userId,
      PlaylistSearch search
  );
}
