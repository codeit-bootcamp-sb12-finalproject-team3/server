package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyPlaybackBroadcaster {
    void broadcastEnded(UUID partyId, WatchPartyPlaybackState state);
}