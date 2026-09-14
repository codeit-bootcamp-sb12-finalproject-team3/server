package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserContentGenrePreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContentGenrePreferenceRepository
        extends JpaRepository<UserContentGenrePreference, UUID> {

    Optional<UserContentGenrePreference> findByUser_IdAndGenre_Id(UUID userId, UUID genreId);

    List<UserContentGenrePreference> findAllByUser_Id(UUID userId);
}
