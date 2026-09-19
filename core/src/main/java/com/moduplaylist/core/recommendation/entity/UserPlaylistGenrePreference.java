package com.moduplaylist.core.recommendation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "user_playlist_genre_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_playlist_genre_preferences",
                columnNames = {"user_id", "genre_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserPlaylistGenrePreference {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false, updatable = false)
    private Genre genre;

    @Column(name = "score", nullable = false)
    private double score;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant scoreUpdatedAt;

    private UserPlaylistGenrePreference(
            User user,
            Genre genre,
            double score,
            Instant scoreUpdatedAt
    ) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.user = Objects.requireNonNull(user);
        this.genre = Objects.requireNonNull(genre);
        this.score = score;
        this.scoreUpdatedAt = Objects.requireNonNull(scoreUpdatedAt);
    }

    public static UserPlaylistGenrePreference create(
            User user,
            Genre genre,
            double initialScore,
            Instant scoreUpdatedAt
    ) {
        return new UserPlaylistGenrePreference(
                user,
                genre,
                initialScore,
                scoreUpdatedAt
        );
    }

    public void updateScore(double score) {
        this.score = score;
    }
}
