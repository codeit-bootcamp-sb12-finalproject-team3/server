package com.moduplaylist.infrastructure.kafka.event;

import java.util.UUID;

public record FollowCreatedKafkaEvent(
        UUID followerId,
        UUID followeeId
) {
}
