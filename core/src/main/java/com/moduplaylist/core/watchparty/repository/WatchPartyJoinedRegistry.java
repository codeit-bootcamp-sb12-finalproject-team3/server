package com.moduplaylist.core.watchparty.repository;

import java.util.Set;
import java.util.UUID;

public interface WatchPartyJoinedRegistry {

    void join(UUID partyId, UUID userId);

    void leave(UUID partyId, UUID userId);

    Set<UUID> findAll(UUID partyId); // ENDED 전이 시 joinedParty 정리용
}