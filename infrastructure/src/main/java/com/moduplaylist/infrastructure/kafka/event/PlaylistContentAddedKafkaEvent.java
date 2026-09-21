package com.moduplaylist.infrastructure.kafka.event;

import java.util.UUID;

public record PlaylistContentAddedKafkaEvent(
    UUID receiverId,
    UUID ownerId,
    UUID playlistId,
    UUID contentId
) {

}
