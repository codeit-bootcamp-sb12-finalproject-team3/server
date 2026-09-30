package com.moduplaylist.core.dm.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "conversation_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationParticipant {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    private ConversationParticipant(Conversation conversation, User user) {
        this.conversation = Objects.requireNonNull(conversation);
        this.user = Objects.requireNonNull(user);
        this.joinedAt = Instant.now();
    }

    public static ConversationParticipant create(Conversation conversation, User user) {
        return new ConversationParticipant(conversation, user);
    }

    @PrePersist
    protected void init() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
