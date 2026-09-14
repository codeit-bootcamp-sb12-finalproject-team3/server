package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Genre;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, UUID> {

    Optional<Genre> findByName(String name);

    Optional<Genre> findByExternalSourceAndExternalId(
            String externalSource,
            int externalId);

    List<Genre> findAllByExternalSourceAndExternalIdIn(
            String externalSource,
            Collection<Integer> externalIds);
}
