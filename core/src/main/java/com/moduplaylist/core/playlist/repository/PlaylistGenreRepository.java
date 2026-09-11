package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistGenre;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistGenreRepository extends JpaRepository<PlaylistGenre, UUID> {

}
