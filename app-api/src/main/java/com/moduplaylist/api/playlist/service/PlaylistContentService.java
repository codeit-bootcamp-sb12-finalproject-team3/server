package com.moduplaylist.api.playlist.service;

import java.util.UUID;

public interface PlaylistContentService {

  void addContent(UUID userId, UUID playlistId, UUID contentId);

  void removeContent(UUID userId, UUID playlistId, UUID contentId);
}
