package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentTag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentTagRepository extends JpaRepository<ContentTag, UUID> {

    @Query("""
            select ct
            from ContentTag ct
            join fetch ct.tag
            where ct.content.id in :contentIds
            """)
    List<ContentTag> findAllWithTagByContentIdIn(@Param("contentIds") Collection<UUID> contentIds);
}
