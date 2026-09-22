package com.moduplaylist.infrastructure.kafka.event;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import java.util.UUID;

public record WatchPartyParticipantChangedKafkaEvent(
        UUID eventId,
        UUID watchPartyId,
        UUID userId,
        ParticipantStatus status,
        boolean isRejoin
) {
}