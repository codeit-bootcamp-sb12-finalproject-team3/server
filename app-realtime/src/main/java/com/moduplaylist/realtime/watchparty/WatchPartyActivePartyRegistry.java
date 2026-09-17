package com.moduplaylist.realtime.watchparty;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only lookup for which party (if any) a user currently has JOINED.
 */
public interface WatchPartyActivePartyRegistry {

    Optional<UUID> findJoinedPartyId(UUID userId);
}