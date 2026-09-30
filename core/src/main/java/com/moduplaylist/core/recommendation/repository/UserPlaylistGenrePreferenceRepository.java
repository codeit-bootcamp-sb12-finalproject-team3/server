package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserPlaylistGenrePreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPlaylistGenrePreferenceRepository
        extends JpaRepository<UserPlaylistGenrePreference, UUID> {

    Optional<UserPlaylistGenrePreference> findByUser_IdAndGenre_Id(UUID userId, UUID genreId);

    List<UserPlaylistGenrePreference> findAllByUser_Id(UUID userId);

    @Query("select distinct preference.user.id from UserPlaylistGenrePreference preference")
    List<UUID> findDistinctUserIds();

    @Query("""
            select distinct preference.user.id
            from UserPlaylistGenrePreference preference
            where preference.score > 0
            """)
    List<UUID> findDistinctUserIdsWithPositiveScore();

    @Query("""
            select preference
            from UserPlaylistGenrePreference preference
            join fetch preference.genre
            where preference.user.id = :userId
            """)
    List<UserPlaylistGenrePreference> findAllWithGenreByUserId(@Param("userId") UUID userId);
}
