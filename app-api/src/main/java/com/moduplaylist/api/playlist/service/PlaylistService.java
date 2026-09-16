package com.moduplaylist.api.playlist.service;

import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import java.util.UUID;

public interface PlaylistService {

  PlaylistResponse create(UUID userId, PlaylistCreateRequest request);

  PlaylistResponse findById(UUID userId, UUID playlistId);

  PlaylistResponse update(UUID userId, UUID playlistId, PlaylistUpdateRequest request);

  void delete(UUID userId, UUID playlistId);
}
