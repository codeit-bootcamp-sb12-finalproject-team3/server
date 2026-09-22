package com.moduplaylist.infrastructure.kafka.event;

import java.util.UUID;

public record PlaylistSubscribedKafkaEvent(
    UUID subscriberId,
    UUID ownerId,
    UUID playlistId
) {
}
