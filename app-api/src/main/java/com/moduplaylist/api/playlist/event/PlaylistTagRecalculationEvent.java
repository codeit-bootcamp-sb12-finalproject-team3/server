package com.moduplaylist.api.playlist.event;

import java.util.UUID;

public record PlaylistTagRecalculationEvent(
    UUID playlistId
) {

}
