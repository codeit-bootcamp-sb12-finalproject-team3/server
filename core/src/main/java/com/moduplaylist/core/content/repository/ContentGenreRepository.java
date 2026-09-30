package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentGenre;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentGenreRepository extends JpaRepository<ContentGenre, UUID> {

    @Query("""
            select cg
            from ContentGenre cg
            join fetch cg.genre
            where cg.content.id in :contentIds
              and cg.content.hidden = false
            """)
    List<ContentGenre> findAllWithGenreByContentIdIn(@Param("contentIds") Collection<UUID> contentIds);

    @Modifying
    @Query("""
            delete from ContentGenre contentGenre
            where contentGenre.content.id = :contentId
            """)
    int deleteAllByContentId(@Param("contentId") UUID contentId);
}
