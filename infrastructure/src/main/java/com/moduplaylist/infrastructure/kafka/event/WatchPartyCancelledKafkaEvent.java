package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WatchPartyCancelledKafkaEvent(
        UUID eventId,
        UUID watchPartyId,
        String title,
        Instant scheduledAt,
        List<UUID> recipientIds
) {
}