package com.moduplaylist.realtime.dm.presence;

import java.util.UUID;

public interface DmActiveConversationRegistry {

    void activate(UUID userId, UUID conversationId, String sessionId);

    void deactivate(UUID userId, UUID conversationId, String sessionId);
}
