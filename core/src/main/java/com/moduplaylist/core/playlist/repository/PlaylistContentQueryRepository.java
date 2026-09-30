package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistContent;
import java.util.List;
import java.util.UUID;

public interface PlaylistContentQueryRepository {

  List<PlaylistContent> findPreviewContents(
      List<UUID> playlistIds,
      int previewLimit
  );
}
