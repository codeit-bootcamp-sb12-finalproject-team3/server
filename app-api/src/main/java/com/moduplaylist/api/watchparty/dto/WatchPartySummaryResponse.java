package com.moduplaylist.api.watchparty.dto;

import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WatchPartySummaryResponse {

    private UUID id;
    private UserSummary host;
    private WatchPartyContentSummary content;
    private String title;
    private Instant scheduledAt;
    private WatchPartyStatus status;
    private Integer maxParticipants;
    private Integer currentParticipantCount;
    private Instant createdAt;
}