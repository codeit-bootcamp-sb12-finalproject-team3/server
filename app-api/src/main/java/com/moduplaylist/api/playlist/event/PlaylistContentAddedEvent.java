package com.moduplaylist.api.playlist.event;

import java.time.Instant;
import java.util.UUID;

public record PlaylistContentAddedEvent(
    UUID ownerId,
    UUID playlistId,
    UUID contentId,
    String playlistTitle,
    String contentTitle,
    Instant occurredAt
) {
}
