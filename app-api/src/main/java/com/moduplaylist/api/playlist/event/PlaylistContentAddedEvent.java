package com.moduplaylist.api.playlist.event;

import java.util.UUID;

public record PlaylistContentAddedEvent(
    UUID ownerId,
    UUID playlistId,
    UUID contentId
) {
}
