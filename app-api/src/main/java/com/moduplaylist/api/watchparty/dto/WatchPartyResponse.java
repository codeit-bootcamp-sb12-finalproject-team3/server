package com.moduplaylist.api.watchparty.dto;

import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public class WatchPartyResponse {

    private UUID id;
    private UserSummary host;
    private WatchPartyContentSummary content;
    private String title;
    private String description;
    private Instant scheduledAt;
    private WatchPartyStatus status;
    private Integer maxParticipants;
    private Integer sessionDurationMinutes;
    private Integer currentParticipantCount; // 저장값 아님 - JOINED 상태 count로 계산
    private Integer startEpisode;
    private Integer endEpisode;
    private Instant createdAt;
    private Instant endedAt;

    public WatchPartyResponse(UUID id, UserSummary host, WatchPartyContentSummary content,
                              String title, String description, Instant scheduledAt,
                              WatchPartyStatus status, Integer maxParticipants, Integer sessionDurationMinutes,
                              Integer currentParticipantCount,
                              Integer startEpisode, Integer endEpisode,
                              Instant createdAt, Instant endedAt) {
        this.id = id;
        this.host = host;
        this.content = content;
        this.title = title;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.maxParticipants = maxParticipants;
        this.sessionDurationMinutes = sessionDurationMinutes;
        this.currentParticipantCount = currentParticipantCount;
        this.startEpisode = startEpisode;
        this.endEpisode = endEpisode;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
    }

}
