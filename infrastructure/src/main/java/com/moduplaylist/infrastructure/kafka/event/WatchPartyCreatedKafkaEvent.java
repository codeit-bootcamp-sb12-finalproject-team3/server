package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record WatchPartyCreatedKafkaEvent(
        UUID eventId,
        UUID watchPartyId,
        UUID hostId,
        UUID contentId,
        Instant scheduledAt
) {
}