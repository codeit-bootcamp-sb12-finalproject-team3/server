package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import java.util.UUID;

public record WatchPartyParticipantChangedEvent(
        UUID eventId,
        UUID partyId,
        UUID userId,
        ParticipantStatus status,
        boolean isRejoin
) {
}