package com.moduplaylist.infrastructure.redis.trending;

import java.util.UUID;

public final class TrendingRedisKey {

    public static final String CONTENTS = "trending:contents";

    private static final String PROCESSED_EVENT_PREFIX = "trending:processed-event:";

    private TrendingRedisKey() {
    }

    public static String processedEvent(UUID eventId) {
        return PROCESSED_EVENT_PREFIX + eventId;
    }
}
