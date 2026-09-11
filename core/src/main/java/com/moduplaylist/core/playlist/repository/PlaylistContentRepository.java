package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistContent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

}
