package com.moduplaylist.core.watchparty.repository;

import java.util.List;
import java.util.UUID;

public interface WatchPartyChatLogRegistry {

    void append(UUID partyId, WatchPartyChatMessage message);

    List<WatchPartyChatMessage> findRecent(UUID partyId, long count);
}