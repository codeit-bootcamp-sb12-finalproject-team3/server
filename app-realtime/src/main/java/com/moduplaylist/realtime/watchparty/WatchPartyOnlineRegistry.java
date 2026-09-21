package com.moduplaylist.realtime.watchparty;

import java.util.UUID;

public interface WatchPartyOnlineRegistry {

    void addOnline(UUID partyId, UUID userId);

    void removeOnline(UUID partyId, UUID userId);
}