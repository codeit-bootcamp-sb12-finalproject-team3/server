package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WatchPartyUpdatedKafkaEvent(
        UUID eventId,
        UUID watchPartyId,
        String title,
        Instant previousScheduledAt,
        Instant scheduledAt,
        List<UUID> recipientIds
) {
}