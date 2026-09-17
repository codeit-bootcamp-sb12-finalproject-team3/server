package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.ContentLike;
import com.moduplaylist.core.content.entity.ContentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentLikeRepository extends JpaRepository<ContentLike, UUID> {

    boolean existsByUser_IdAndContent_Id(UUID userId, UUID contentId);

    @Query("""
            select contentLike.content.id
            from ContentLike contentLike
            where contentLike.user.id = :userId
            """)
    List<UUID> findContentIdsByUserId(@Param("userId") UUID userId);

    @Query("""
            select
                case when count(contentLike) > 0 then true else false end as liked,
                content.type as contentType,
                content.likeCount as likeCount
            from Content content
            left join ContentLike contentLike
                on contentLike.content = content
                and contentLike.user.id = :userId
            where content.id = :contentId
              and content.hidden = false
            group by content.id, content.type, content.likeCount
            """)
    Optional<LikeStatus> findStatus(
            @Param("userId") UUID userId,
            @Param("contentId") UUID contentId);

    @Modifying
    @Query("""
            delete from ContentLike contentLike
            where contentLike.user.id = :userId
              and contentLike.content.id = :contentId
            """)
    int deleteByUserIdAndContentId(
            @Param("userId") UUID userId,
            @Param("contentId") UUID contentId);

    @Modifying
    @Query("""
            update Content content
            set content.likeCount = content.likeCount + 1
            where content.id = :contentId
              and content.hidden = false
            """)
    int incrementLikeCount(@Param("contentId") UUID contentId);

    @Modifying
    @Query("""
            update Content content
            set content.likeCount = content.likeCount - 1
            where content.id = :contentId
              and content.likeCount > 0
            """)
    int decrementLikeCount(@Param("contentId") UUID contentId);

    interface LikeStatus {

        boolean getLiked();

        ContentType getContentType();

        long getLikeCount();
    }
}