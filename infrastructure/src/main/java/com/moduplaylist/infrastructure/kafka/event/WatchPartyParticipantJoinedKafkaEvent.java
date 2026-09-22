package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record WatchPartyParticipantJoinedKafkaEvent(
        UUID eventId,
        UUID watchPartyId,
        UUID userId,
        UUID contentId,
        Instant occurredAt
) {
}
