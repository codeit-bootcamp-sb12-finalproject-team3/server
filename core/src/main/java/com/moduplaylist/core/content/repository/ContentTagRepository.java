package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.TagSource;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentTagRepository extends JpaRepository<ContentTag, UUID> {

    @Query("""
            select ct
            from ContentTag ct
            join fetch ct.tag
            where ct.content.id in :contentIds
              and ct.content.hidden = false
            """)
    List<ContentTag> findAllWithTagByContentIdIn(@Param("contentIds") Collection<UUID> contentIds);

    @Query("""
            select ct.tag.name
            from ContentTag ct
            where ct.content.parentContent.id = :parentContentId
              and ct.content.id <> :contentId
              and ct.content.hidden = false
            group by ct.tag.id, ct.tag.name
            order by max(case when ct.source = :manualSource then 1 else 0 end) desc,
                     count(distinct ct.content.id) desc,
                     ct.tag.name asc
            """)
    List<String> findCanonicalNamesFromSiblingSeasons(
            @Param("parentContentId") UUID parentContentId,
            @Param("contentId") UUID contentId,
            @Param("manualSource") TagSource manualSource,
            Pageable pageable);
}
