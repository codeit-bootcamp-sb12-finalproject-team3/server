package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyHostRegistry {

    void setHost(UUID partyId, UUID hostId);
}