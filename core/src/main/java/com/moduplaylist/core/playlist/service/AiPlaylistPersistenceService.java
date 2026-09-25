package com.moduplaylist.core.playlist.service;

import com.moduplaylist.core.playlist.entity.Playlist;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiPlaylistPersistenceService {

  private final PlaylistCreationService playlistCreationService;
  private final PlaylistTagAssignmentService playlistTagAssignmentService;

  @Transactional
  public Playlist save(
      UUID ownerId,
      String title,
      String description,
      List<UUID> contentIds,
      List<String> tags
  ) {
    Playlist playlist = playlistCreationService.create(
        ownerId,
        title,
        description,
        contentIds
    );

    playlistTagAssignmentService.assign(playlist.getId(), tags);

    return playlist;
  }
}
