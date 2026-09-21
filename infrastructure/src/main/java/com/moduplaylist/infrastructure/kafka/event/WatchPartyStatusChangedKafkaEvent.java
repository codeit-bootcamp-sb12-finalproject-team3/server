package com.moduplaylist.infrastructure.kafka.event;

import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import java.util.UUID;

public record WatchPartyStatusChangedKafkaEvent(
        UUID watchPartyId,
        WatchPartyStatus status
) {
}