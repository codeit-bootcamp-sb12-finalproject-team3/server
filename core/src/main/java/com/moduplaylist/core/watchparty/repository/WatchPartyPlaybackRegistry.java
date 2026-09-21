package com.moduplaylist.core.watchparty.repository;

import java.util.Optional;
import java.util.UUID;

public interface WatchPartyPlaybackRegistry {

    void createOnLive(UUID partyId, WatchPartyPlaybackState state);

    Optional<WatchPartyPlaybackState> find(UUID partyId);

    void markEnded(UUID partyId);

}