package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentOtt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentOttRepository extends JpaRepository<ContentOtt, UUID> {

    @Query("""
            select contentOtt
            from ContentOtt contentOtt
            join fetch contentOtt.ottPlatform ottPlatform
            where contentOtt.content.id = :contentId
            order by ottPlatform.name asc, ottPlatform.id asc
            """)
    List<ContentOtt> findAllWithPlatformByContentId(
            @Param("contentId") UUID contentId);

    Optional<ContentOtt> findByContent_IdAndOttPlatform_Id(
            UUID contentId,
            UUID ottPlatformId);

    @Modifying
    @Query("""
            delete from ContentOtt contentOtt
            where contentOtt.content.id = :contentId
            """)
    int deleteAllByContentId(@Param("contentId") UUID contentId);
}
