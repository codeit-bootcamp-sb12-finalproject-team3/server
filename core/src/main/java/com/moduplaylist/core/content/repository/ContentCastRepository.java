package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentCast;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentCastRepository extends JpaRepository<ContentCast, UUID> {

    List<ContentCast> findAllByContent_IdOrderByDisplayOrderAsc(UUID contentId);

    @Modifying
    @Query("""
            delete from ContentCast contentCast
            where contentCast.content.id = :contentId
            """)
    int deleteAllByContentId(@Param("contentId") UUID contentId);
}
