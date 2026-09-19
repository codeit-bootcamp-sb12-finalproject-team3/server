package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserPlaylistTagPreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPlaylistTagPreferenceRepository
        extends JpaRepository<UserPlaylistTagPreference, UUID> {

    Optional<UserPlaylistTagPreference> findByUser_IdAndTag_Id(UUID userId, UUID tagId);

    List<UserPlaylistTagPreference> findAllByUser_Id(UUID userId);

    @Query("""
            select preference
            from UserPlaylistTagPreference preference
            join fetch preference.tag
            where preference.user.id = :userId
            """)
    List<UserPlaylistTagPreference> findAllWithTagByUserId(@Param("userId") UUID userId);
}
