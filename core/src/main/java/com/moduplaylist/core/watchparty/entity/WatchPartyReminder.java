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
@Table(name = "watch_party_reminders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchPartyReminder {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watch_party_id", nullable = false, updatable = false)
    private WatchParty watchParty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WatchPartyReminder(WatchParty watchParty, User user) {
        this.watchParty = Objects.requireNonNull(watchParty);
        this.user = Objects.requireNonNull(user);
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void init() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}