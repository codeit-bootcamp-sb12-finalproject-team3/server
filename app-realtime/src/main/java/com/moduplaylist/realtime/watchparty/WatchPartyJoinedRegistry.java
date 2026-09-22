package com.moduplaylist.realtime.watchparty;

import java.util.UUID;

/**
 * Read-only joined-status contract needed by the WebSocket authorization layer.
 */
public interface WatchPartyJoinedRegistry {

    boolean isJoined(UUID partyId, UUID userId);
}