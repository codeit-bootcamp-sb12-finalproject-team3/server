package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import java.util.UUID;

public record WatchPartyParticipantChangedEvent(
        UUID partyId,
        UUID userId,
        ParticipantStatus status
) {
}