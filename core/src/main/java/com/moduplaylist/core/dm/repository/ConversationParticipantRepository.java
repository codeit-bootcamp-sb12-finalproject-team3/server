package com.moduplaylist.core.dm.repository;

import com.moduplaylist.core.dm.entity.Conversation;
import com.moduplaylist.core.dm.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, UUID> {

    boolean existsByConversation_IdAndUser_Id(UUID conversationId, UUID userId);

    List<ConversationParticipant> findByConversation_Id(UUID conversationId);

    Optional<ConversationParticipant> findByConversation_IdAndUser_Id(
            UUID conversationId,
            UUID userId
    );

    @Query("""
            SELECT c AS conversation,
                   peerParticipant.user AS peer,
                   latestMessage AS latestMessage
            FROM ConversationParticipant currentParticipant
            JOIN currentParticipant.conversation c
            JOIN ConversationParticipant peerParticipant
              ON peerParticipant.conversation = c
             AND peerParticipant.user.id <> :userId
            LEFT JOIN DirectMessage latestMessage
              ON latestMessage.conversation = c
             AND NOT EXISTS (
                SELECT newerMessage.id
                FROM DirectMessage newerMessage
                WHERE newerMessage.conversation = c
                  AND (
                    newerMessage.createdAt > latestMessage.createdAt
                    OR (
                      newerMessage.createdAt = latestMessage.createdAt
                      AND newerMessage.id > latestMessage.id
                    )
                  )
             )
            WHERE currentParticipant.user.id = :userId
              AND (
                :cursorUpdatedAt IS NULL
                OR c.updatedAt < :cursorUpdatedAt
                OR (c.updatedAt = :cursorUpdatedAt AND c.id < :cursorId)
              )
            ORDER BY c.updatedAt DESC, c.id DESC
            """)
    Slice<ConversationListItem> findConversationPage(
            @Param("userId") UUID userId,
            @Param("cursorUpdatedAt") Instant cursorUpdatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    long countByUser_Id(UUID userId);

    @Query("""
            SELECT c
            FROM Conversation c
            WHERE (
                SELECT COUNT(cp)
                FROM ConversationParticipant cp
                WHERE cp.conversation = c
            ) = 2 
              AND EXISTS (
                SELECT cp1.id
                FROM ConversationParticipant cp1
                WHERE cp1.conversation = c AND cp1.user.id = :firstUserId
              )
              AND EXISTS (
                SELECT cp2.id
                FROM ConversationParticipant cp2
                WHERE cp2.conversation = c AND cp2.user.id = :secondUserId
              )
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<Conversation> findDirectConversationsBetween(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );
}
