package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserPlaylistTagPreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPlaylistTagPreferenceRepository
        extends JpaRepository<UserPlaylistTagPreference, UUID> {

    Optional<UserPlaylistTagPreference> findByUser_IdAndTag_Id(UUID userId, UUID tagId);

    List<UserPlaylistTagPreference> findAllByUser_Id(UUID userId);
}
