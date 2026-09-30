package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyKeyLifecycleRegistry {
    void armSafetyNetTtl(UUID partyId);
    void deletePartyKeysNow(UUID partyId);
}