package com.moduplaylist.infrastructure.kafka.event;

import java.util.UUID;

public record PlaylistContentAddedKafkaEvent(
    UUID ownerId,
    UUID playlistId,
    UUID contentId,
    String playlistTitle,
    String contentTitle
) {

}
