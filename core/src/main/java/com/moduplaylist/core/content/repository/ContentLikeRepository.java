package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentLike;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentLikeRepository extends JpaRepository<ContentLike, UUID> {

    @Query("select contentLike.content.id from ContentLike contentLike "
            + "where contentLike.userId = :userId")
    List<UUID> findContentIdsByUserId(@Param("userId") UUID userId);
}
