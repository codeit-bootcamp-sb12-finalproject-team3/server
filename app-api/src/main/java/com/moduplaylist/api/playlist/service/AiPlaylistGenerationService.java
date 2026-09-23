package com.moduplaylist.api.playlist.service;

import com.moduplaylist.api.playlist.dto.AiPlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import java.util.UUID;

public interface AiPlaylistGenerationService {

  PlaylistResponse create(UUID userId, AiPlaylistCreateRequest request);

}
