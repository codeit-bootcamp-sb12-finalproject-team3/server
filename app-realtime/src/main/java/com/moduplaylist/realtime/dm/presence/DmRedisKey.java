package com.moduplaylist.realtime.dm.presence;

import java.util.Objects;
import java.util.UUID;

final class DmRedisKey {

    private DmRedisKey() {
    }

    static String activeConversation(UUID userId, UUID conversationId) {
        return "dm:active:" + require(userId) + ":" + require(conversationId);
    }

    private static UUID require(UUID id) {
        return Objects.requireNonNull(id, "id must not be null");
    }
}
