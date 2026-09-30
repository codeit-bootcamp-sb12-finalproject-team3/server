package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistGenre;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistGenreRepository extends JpaRepository<PlaylistGenre, UUID> {

  @Query("""
      select playlistGenre
      from PlaylistGenre playlistGenre
      join fetch playlistGenre.genre
      where playlistGenre.playlist.id = :playlistId
      """)
  List<PlaylistGenre> findAllWithGenreByPlaylistId(@Param("playlistId") UUID playlistId);
}
