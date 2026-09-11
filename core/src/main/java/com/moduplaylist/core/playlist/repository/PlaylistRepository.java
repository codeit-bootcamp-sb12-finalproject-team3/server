package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.Playlist;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

}
