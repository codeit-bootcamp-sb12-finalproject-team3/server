package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyPlaybackBroadcaster {

    void broadcastStarted(UUID partyId, WatchPartyPlaybackState state);


    void broadcastEnded(UUID partyId, WatchPartyPlaybackState state);
}