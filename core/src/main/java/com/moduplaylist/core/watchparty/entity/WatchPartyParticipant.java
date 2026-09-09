package com.moduplaylist.core.watchparty.entity;

import com.github.f4b6a3.uuid.UuidCreator;
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
@Table(name = "watch_party_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchPartyParticipant {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watch_party_id", nullable = false, updatable = false)
    private WatchParty watchParty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    public WatchPartyParticipant(User user, WatchParty watchParty) {
        this.user = Objects.requireNonNull(user);
        this.watchParty = Objects.requireNonNull(watchParty);
        this.status = ParticipantStatus.JOINED;
        this.joinedAt = Instant.now();
    }

    @PrePersist
    protected void init() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    public void leave() {
        if (this.status != ParticipantStatus.JOINED) return;
        this.status = ParticipantStatus.LEFT;
        this.leftAt = Instant.now();
    }

    public void kick() {
        if (this.status != ParticipantStatus.JOINED) return;
        this.status = ParticipantStatus.KICKED;
        this.leftAt = Instant.now();
    }
}