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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;


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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Content content set content.title = :title, content.description = :description, "
            + "content.thumbnailUrl = :thumbnailUrl "
            + "where content.id = :contentId")
    int updateSportCommonDetails(
            @Param("contentId") UUID contentId,
            @Param("title") String title,
            @Param("description") String description,
            @Param("thumbnailUrl") String thumbnailUrl);

    Optional<Content> findByExternalSourceAndTypeAndExternalId(
            String externalSource, ContentType type, Integer externalId);

    Optional<Content> findByIdAndHiddenFalse(UUID contentId);

    List<Content> findAllByExternalSourceAndTypeAndExternalIdIn(
            String externalSource, ContentType type, Collection<Integer> externalIds);

    List<Content> findAllByParentContent_IdAndHiddenFalseOrderBySeasonNumberAsc(
            UUID parentContentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Content> findByParentContent_IdAndSeasonNumber(
            UUID parentContentId,
            Integer seasonNumber);

    boolean existsByTypeAndTitleAndReleaseDate(
            ContentType type,
            String title,
            java.time.LocalDate releaseDate);

    boolean existsByTypeAndTitleAndReleaseDateAndIdNot(
            ContentType type,
            String title,
            java.time.LocalDate releaseDate,
            UUID contentId);

    boolean existsByTypeAndTitle(ContentType type, String title);

    boolean existsByTypeAndTitleAndIdNot(
            ContentType type,
            String title,
            UUID contentId);

    @Query("""
            select content
            from Content content
            where content.type = :type
              and lower(content.title) like lower(concat('%', :query, '%'))
            order by content.title asc, content.id asc
            """)
    List<Content> searchSeriesForAdmin(
            @Param("type") ContentType type,
            @Param("query") String query,
            Pageable pageable);
}
