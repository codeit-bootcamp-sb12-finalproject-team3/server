package com.moduplaylist.core.dm.repository;

import com.moduplaylist.core.dm.entity.Conversation;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Conversation c
            SET c.updatedAt = :updatedAt
            WHERE c.id = :conversationId
              AND c.updatedAt < :updatedAt
            """)
    int updateUpdatedAtIfOlder(
            @Param("conversationId") UUID conversationId,
            @Param("updatedAt") Instant updatedAt
    );
}
