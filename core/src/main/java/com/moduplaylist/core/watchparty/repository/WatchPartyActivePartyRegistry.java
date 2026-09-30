package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyActivePartyRegistry {

    void setJoinedParty(UUID userId, UUID partyId);

    void clearJoinedParty(UUID userId);
}