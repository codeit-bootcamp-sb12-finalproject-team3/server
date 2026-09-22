package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record WatchPartyReminderDueKafkaEvent(
        UUID eventId,
        UUID watchPartyId,
        UUID userId,
        Instant scheduledAt
) {
}