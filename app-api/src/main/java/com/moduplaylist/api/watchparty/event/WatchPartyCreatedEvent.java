package com.moduplaylist.api.watchparty.event;

import java.time.Instant;
import java.util.UUID;

public record WatchPartyCreatedEvent(
        UUID eventId,
        UUID partyId,
        UUID hostId,
        UUID contentId,
        Instant scheduledAt
) {
}