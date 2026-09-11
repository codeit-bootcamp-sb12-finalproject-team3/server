package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ContentRepository extends JpaRepository<Content, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select content from Content content where content.id = :contentId")
    Optional<Content> findByIdForUpdate(@Param("contentId") UUID contentId);

    Optional<Content> findByExternalSourceAndTypeAndExternalId(
            String externalSource, ContentType type, Integer externalId);

    List<Content> findAllByExternalSourceAndTypeAndExternalIdIn(
            String externalSource, ContentType type, Collection<Integer> externalIds);

    List<Content> findAllByParentContent_IdOrderBySeasonNumberAsc(UUID parentContentId);

    boolean existsByParentContent_IdAndSeasonNumber(UUID parentContentId, Integer seasonNumber);
}
