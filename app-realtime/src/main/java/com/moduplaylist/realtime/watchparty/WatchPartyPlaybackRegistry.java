package com.moduplaylist.realtime.watchparty;

import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackState;
import java.util.Optional;
import java.util.UUID;

public interface WatchPartyPlaybackRegistry {

    Optional<WatchPartyPlaybackState> find(UUID partyId);

    void update(UUID partyId, WatchPartyPlaybackState state);
}