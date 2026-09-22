package com.moduplaylist.api.playlist.event;

import java.util.UUID;

public record PlaylistSubscribedEvent(
    UUID subscriberId,
    UUID ownerId,
    UUID playlistId
) {
}
