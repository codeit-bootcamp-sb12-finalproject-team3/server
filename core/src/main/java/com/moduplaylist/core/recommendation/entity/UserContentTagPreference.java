package com.moduplaylist.core.recommendation.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.content.entity.Tag;
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
        name = "user_content_tag_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_content_tag_preferences",
                        columnNames = {"user_id", "tag_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserContentTagPreference {

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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserContentTagPreference(
            User user,
            Tag tag,
            double score
    ) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.user = user;
        this.tag = tag;
        this.score = score;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateScore(double score) {
        this.score = score;
        this.updatedAt = LocalDateTime.now();
    }

    public void addScore(double delta) {
        this.score += delta;
        this.updatedAt = LocalDateTime.now();
    }
}