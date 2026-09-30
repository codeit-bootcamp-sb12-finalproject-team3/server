package com.moduplaylist.realtime.watchparty;

import com.moduplaylist.realtime.watchparty.dto.WatchPartyChatMessage;
import java.util.UUID;

public interface WatchPartyChatLogRegistry {

    void append(UUID partyId, WatchPartyChatMessage message);
}