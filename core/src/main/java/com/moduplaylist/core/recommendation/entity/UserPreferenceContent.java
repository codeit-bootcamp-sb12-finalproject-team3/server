package com.moduplaylist.core.recommendation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "user_preference_contents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_preference_contents",
                        columnNames = {"user_id", "content_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceContent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false, updatable = false)
    private Content content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserPreferenceContent(User user, Content content) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.user = user;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}