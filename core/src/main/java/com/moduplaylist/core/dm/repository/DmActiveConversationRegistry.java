package com.moduplaylist.core.dm.repository;

import java.util.UUID;

public interface DmActiveConversationRegistry {

    boolean isActive(UUID userId, UUID conversationId);
}
