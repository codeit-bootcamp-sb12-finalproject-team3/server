package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByName(String name);

    List<Tag> findAllByNameIn(Collection<String> names);

    @Modifying
    @Query(value = """
            insert into tags (id, name)
            values (:tagId, :name)
            on duplicate key update name = tags.name
            """, nativeQuery = true)
    int upsert(
            @Param("tagId") UUID tagId,
            @Param("name") String name);
}
