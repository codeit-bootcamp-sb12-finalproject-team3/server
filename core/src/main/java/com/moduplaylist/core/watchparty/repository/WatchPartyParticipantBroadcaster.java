package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyParticipantBroadcaster {
    void broadcast(UUID partyId, WatchPartyParticipantChangedMessage message);
}