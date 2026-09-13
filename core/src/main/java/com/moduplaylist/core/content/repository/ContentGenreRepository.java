package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentGenre;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentGenreRepository extends JpaRepository<ContentGenre, UUID> {

    @Query("""
            select cg
            from ContentGenre cg
            join fetch cg.genre
            where cg.content.id in :contentIds
            """)
    List<ContentGenre> findAllWithGenreByContentIdIn(@Param("contentIds") Collection<UUID> contentIds);
}
