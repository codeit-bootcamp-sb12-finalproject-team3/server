package com.moduplaylist.infrastructure.kafka.event;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import java.util.UUID;

public record WatchPartyParticipantChangedKafkaEvent(
        UUID watchPartyId,
        UUID userId,
        ParticipantStatus status
) {
}