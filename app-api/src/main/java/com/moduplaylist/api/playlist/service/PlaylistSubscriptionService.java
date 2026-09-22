package com.moduplaylist.api.playlist.service;

import java.util.UUID;

public interface PlaylistSubscriptionService {

  void subscribe(UUID userId, UUID playlistId);

  void unsubscribe(UUID userId, UUID playlistId);
}
