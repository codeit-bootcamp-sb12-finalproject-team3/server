package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistTag;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistTagRepository extends JpaRepository<PlaylistTag, UUID> {

}
