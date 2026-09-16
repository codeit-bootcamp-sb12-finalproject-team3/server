package com.moduplaylist.core.dm.repository;

import com.moduplaylist.core.dm.entity.Conversation;
import com.moduplaylist.core.dm.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
            SELECT cp.conversation
            FROM ConversationParticipant cp
            WHERE cp.user.id = :userId
            ORDER BY cp.conversation.updatedAt DESC, cp.conversation.id DESC
            """)
    List<Conversation> findConversationsByUserId(@Param("userId") UUID userId);

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
