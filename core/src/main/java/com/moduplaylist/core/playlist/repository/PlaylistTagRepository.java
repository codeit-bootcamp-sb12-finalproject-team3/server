package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistTag;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistTagRepository extends JpaRepository<PlaylistTag, UUID> {

  @Query("""
      select playlistTag
      from PlaylistTag playlistTag
      join fetch playlistTag.tag
      where playlistTag.playlist.id = :playlistId
      """)
  List<PlaylistTag> findAllWithTagByPlaylistId(@Param("playlistId") UUID playlistId);
}
