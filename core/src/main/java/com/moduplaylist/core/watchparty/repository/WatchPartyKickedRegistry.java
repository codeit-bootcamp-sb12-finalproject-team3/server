package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyKickedRegistry {

    void kick(UUID partyId, UUID userId);

    boolean isKicked(UUID partyId, UUID userId);
}