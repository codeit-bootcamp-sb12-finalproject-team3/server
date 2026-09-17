package com.moduplaylist.realtime.watchparty;

import java.util.UUID;

/**
 * Read-only host-identity contract needed by the WebSocket authorization layer.
 */
public interface WatchPartyHostRegistry {

    boolean isHost(UUID partyId, UUID userId);
}