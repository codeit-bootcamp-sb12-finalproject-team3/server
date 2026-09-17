package com.moduplaylist.core.dm.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "direct_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DirectMessage {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private User sender;

    @Column(name = "content", nullable = false, length = 255)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    private DirectMessage(Conversation conversation, User sender, String content) {
        this.conversation = Objects.requireNonNull(conversation);
        this.sender = Objects.requireNonNull(sender);
        this.content = Objects.requireNonNull(content);
    }

    public static DirectMessage create(Conversation conversation, User sender, String content) {
        return new DirectMessage(conversation, sender, content);
    }

    @PrePersist
    protected void init() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    public void markAsRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = Objects.requireNonNull(readAt);
        }
    }
}
