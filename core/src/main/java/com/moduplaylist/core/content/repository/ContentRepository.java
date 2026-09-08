package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.Content;
import com.moduplaylist.core.content.ContentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Content c where c.id = :id")
    Optional<Content> findByIdForUpdate(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Content c where c.id = :id")
    void deleteByIdInDatabase(@Param("id") UUID id);

    Optional<Content> findByExternalSourceAndTypeAndExternalId(
            String externalSource, ContentType type, Integer externalId);

    List<Content> findAllByParentContent_IdOrderBySeasonNumberAsc(UUID parentContentId);

    boolean existsByParentContent_IdAndSeasonNumber(UUID parentContentId, Integer seasonNumber);
}
