package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistContent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

  @EntityGraph(attributePaths = "content")
  List<PlaylistContent> findAllByPlaylist_IdOrderByCreatedAtAscIdAsc(UUID playlistId);
}
