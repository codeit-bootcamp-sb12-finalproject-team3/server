package com.moduplaylist.api.watchparty.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WatchPartyCancelledEvent(
        UUID eventId,
        UUID partyId,
        String title,
        Instant scheduledAt,
        List<UUID> recipientIds
) {
}