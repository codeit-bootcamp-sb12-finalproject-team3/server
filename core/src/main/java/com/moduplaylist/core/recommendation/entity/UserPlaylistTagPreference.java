package com.moduplaylist.core.recommendation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_playlist_tag_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserPlaylistTagPreference {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false, updatable = false)
    private Tag tag;

    @Column(name = "score", nullable = false)
    private double score;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant scoreUpdatedAt;

    private UserPlaylistTagPreference(
            User user,
            Tag tag,
            double score,
            Instant scoreUpdatedAt
    ) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.user = Objects.requireNonNull(user);
        this.tag = Objects.requireNonNull(tag);
        this.score = score;
        this.scoreUpdatedAt = Objects.requireNonNull(scoreUpdatedAt);
    }

    public static UserPlaylistTagPreference create(
            User user,
            Tag tag,
            double initialScore,
            Instant scoreUpdatedAt
    ) {
        return new UserPlaylistTagPreference(
                user,
                tag,
                initialScore,
                scoreUpdatedAt
        );
    }

    public void updateScore(double score) {
        this.score = score;
    }

}
