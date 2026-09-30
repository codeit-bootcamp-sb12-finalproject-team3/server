package com.moduplaylist.realtime.watchparty;

import java.util.UUID;

/**
 * Read-only kicked-status contract needed by the WebSocket authorization layer.
 */
public interface WatchPartyKickedRegistry {

    boolean isKicked(UUID partyId, UUID userId);
}