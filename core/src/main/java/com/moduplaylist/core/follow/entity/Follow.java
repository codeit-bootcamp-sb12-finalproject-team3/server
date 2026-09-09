package com.moduplaylist.core.follow.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "follows")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow{

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false, updatable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false, updatable = false)
    private User followee;

    public Follow(User follower, User followee) {
        this.follower = Objects.requireNonNull(follower);
        this.followee = Objects.requireNonNull(followee);
    }

    @PrePersist
    protected void init() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}

