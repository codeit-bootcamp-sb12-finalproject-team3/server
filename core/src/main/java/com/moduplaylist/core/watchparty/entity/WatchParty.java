package com.moduplaylist.core.watchparty.entity;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.watchparty.exception.WatchPartyInvalidStateException;
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

    @Column(name = "session_duration_minutes", nullable = false)
    private Integer sessionDurationMinutes;

    @Column(name = "start_episode")
    private Integer startEpisode;

    @Column(name = "end_episode")
    private Integer endEpisode;

    @Column(name = "ended_at")
    private Instant endedAt;

    public void start() {
        if (this.status != WatchPartyStatus.SCHEDULED) {
            throw new WatchPartyInvalidStateException(this.getId(), "시작 대기 상태의 방만 시작할 수 있습니다.");
        }
        this.status = WatchPartyStatus.LIVE;
    }

    public void end() {
        if (this.status != WatchPartyStatus.LIVE) {
            throw new WatchPartyInvalidStateException(this.getId(), "진행 중인 방만 종료할 수 있습니다.");
        }
        this.status = WatchPartyStatus.ENDED;
        this.endedAt = Instant.now();
    }

    public void validateEditable() {
        if (this.status != WatchPartyStatus.SCHEDULED) {
            throw new WatchPartyInvalidStateException(this.getId(), "시작 대기 상태의 방만 수정/삭제할 수 있습니다.");
        }
    }

    public void update(String title, String description, Instant scheduledAt,
                       Integer maxParticipants, Integer sessionDurationMinutes,
                       Integer startEpisode, Integer endEpisode) {
        validateEditable();
        this.title = title;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.maxParticipants = maxParticipants;
        this.sessionDurationMinutes = sessionDurationMinutes;
        this.startEpisode = startEpisode;
        this.endEpisode = endEpisode;
    }
}