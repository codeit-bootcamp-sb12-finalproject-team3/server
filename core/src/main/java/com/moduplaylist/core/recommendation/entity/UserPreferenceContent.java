package com.moduplaylist.core.recommendation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_preference_contents")
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
    private Instant createdAt;

    private UserPreferenceContent(User user, Content content) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.user = Objects.requireNonNull(user);
        this.content = Objects.requireNonNull(content);
        this.createdAt = Instant.now();
    }

    public static UserPreferenceContent create(User user, Content content) {
        return new UserPreferenceContent(user, content);
    }
}
