package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyOnlineRegistry {

    void addOnline(UUID partyId, UUID userId);

    void removeOnline(UUID partyId, UUID userId);
}