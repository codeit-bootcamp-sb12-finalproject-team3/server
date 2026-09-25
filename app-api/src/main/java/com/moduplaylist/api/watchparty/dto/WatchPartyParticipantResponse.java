package com.moduplaylist.api.watchparty.dto;

import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WatchPartyParticipantResponse {

    private UserSummary user;
    private Instant joinedAt;

    public static WatchPartyParticipantResponse from(WatchPartyParticipant participant) {
        return WatchPartyParticipantResponse.builder()
                .user(UserSummary.from(participant.getUser()))
                .joinedAt(participant.getJoinedAt())
                .build();
    }
}