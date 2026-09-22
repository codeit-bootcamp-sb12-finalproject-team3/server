package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.entity.ContentType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenreRepository extends JpaRepository<Genre, UUID> {

    Optional<Genre> findByName(String name);

    Optional<Genre> findByExternalSourceAndExternalId(
            String externalSource,
            int externalId);

    List<Genre> findAllByExternalSourceAndExternalIdIn(
            String externalSource,
            Collection<Integer> externalIds);

    List<Genre> findAllByOrderByNameAsc();

    @Query("""
            select distinct contentGenre.genre
            from ContentGenre contentGenre
            where contentGenre.content.type = :contentType
              and contentGenre.content.hidden = false
            order by contentGenre.genre.name asc
            """)
    List<Genre> findAllUsedByContentType(
            @Param("contentType") ContentType contentType);
}
