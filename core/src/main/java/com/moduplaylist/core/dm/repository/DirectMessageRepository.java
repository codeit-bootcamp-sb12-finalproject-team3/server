package com.moduplaylist.core.dm.repository;

import com.moduplaylist.core.dm.entity.DirectMessage;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    Slice<DirectMessage> findByConversation_IdOrderByCreatedAtDescIdDesc(
            UUID conversationId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM DirectMessage m
            WHERE m.conversation.id = :conversationId
              AND (
                m.createdAt < :cursorCreatedAt
                OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId)
              )
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    Slice<DirectMessage> findNextPage(
            @Param("conversationId") UUID conversationId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    Optional<DirectMessage> findFirstByConversation_IdOrderByCreatedAtDescIdDesc(
            UUID conversationId
    );

    Optional<DirectMessage> findByIdAndConversation_Id(UUID messageId, UUID conversationId);

    long countByConversation_Id(UUID conversationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DirectMessage m
            SET m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :readerId
              AND m.readAt IS NULL
              AND (
                m.createdAt < :lastReadCreatedAt
                OR (
                  m.createdAt = :lastReadCreatedAt
                  AND m.id <= :lastReadMessageId
                )
              )
            """)
    int markMessagesAsRead(
            @Param("conversationId") UUID conversationId,
            @Param("readerId") UUID readerId,
            @Param("lastReadCreatedAt") Instant lastReadCreatedAt,
            @Param("lastReadMessageId") UUID lastReadMessageId,
            @Param("readAt") Instant readAt
    );
}
