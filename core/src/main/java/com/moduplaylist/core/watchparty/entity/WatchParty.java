package com.moduplaylist.core.watchparty.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "watch_parties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class WatchParty extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false, updatable = false)
    private User host;

    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WatchPartyStatus status = WatchPartyStatus.SCHEDULED;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "start_episode")
    private Integer startEpisode;

    @Column(name = "end_episode")
    private Integer endEpisode;

    @Column(name = "ended_at")
    private Instant endedAt;
}