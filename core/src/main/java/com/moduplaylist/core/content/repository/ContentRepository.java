package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ContentRepository extends JpaRepository<Content, UUID>, ContentQueryRepository {

    @Query("select content.id from Content content")
    List<UUID> findAllIds();

    @Query("""
            select content
            from Content content
            where content.type in :types
              and content.updatedAt <= :through
            order by content.updatedAt, content.id
            """)
    List<Content> findEmbeddingSourcesThrough(
            @Param("types") Collection<ContentType> types,
            @Param("through") Instant through
    );

    @Query("""
            select content
            from Content content
            where content.type in :types
              and content.updatedAt > :after
              and content.updatedAt <= :through
            order by content.updatedAt, content.id
            """)
    List<Content> findModifiedEmbeddingSources(
            @Param("types") Collection<ContentType> types,
            @Param("after") Instant after,
            @Param("through") Instant through
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select content from Content content where content.id = :contentId")
    Optional<Content> findByIdForUpdate(@Param("contentId") UUID contentId);

    Optional<Content> findByExternalSourceAndTypeAndExternalId(
            String externalSource, ContentType type, Integer externalId);

    Optional<Content> findByIdAndHiddenFalse(UUID contentId);

    List<Content> findAllByExternalSourceAndTypeAndExternalIdIn(
            String externalSource, ContentType type, Collection<Integer> externalIds);

    List<Content> findAllByParentContent_IdAndHiddenFalseOrderBySeasonNumberAsc(
            UUID parentContentId);

    boolean existsByParentContent_IdAndSeasonNumber(UUID parentContentId, Integer seasonNumber);

    boolean existsByParentContent_IdAndSeasonNumberAndIdNot(
            UUID parentContentId,
            Integer seasonNumber,
            UUID contentId);
}
