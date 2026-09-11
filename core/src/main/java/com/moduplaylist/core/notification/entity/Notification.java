package com.moduplaylist.core.notification.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @Column(name ="id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false, updatable = false)
    private User receiver;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private NotificationLevel level;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Notification create(
            User receiver,
            String title,
            String content,
            NotificationLevel level
    ) {
        Notification notification = new Notification();
        notification.receiver = receiver;
        notification.title = title;
        notification.content = content;
        notification.level = level;
        return notification;
    }

    @PrePersist
    protected void init() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}


//id                  BINARY(16) NOT NULL,
//receiver_id         BINARY(16) NOT NULL,
//title               VARCHAR(100) NOT NULL,
//content             VARCHAR(500) NOT NULL,
//level               ENUM('INFO', 'WARNING', 'ERROR') NOT NULL // DEFAULT 'INFO',
//created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)