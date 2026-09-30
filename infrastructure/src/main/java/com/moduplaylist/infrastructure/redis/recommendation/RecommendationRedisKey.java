package com.moduplaylist.infrastructure.redis.recommendation;

import java.util.Objects;
import java.util.UUID;

public final class RecommendationRedisKey {

    private static final String CONTENTS_PREFIX = "recommendation:contents:";
    private static final String PLAYLISTS_PREFIX = "recommendation:playlists:";
    private static final String REFRESH_ACTIVITY_PREFIX = "recommendation:refresh:activity:";
    private static final String REFRESH_LOCK_PREFIX = "recommendation:refresh:lock:";

    private RecommendationRedisKey() {
    }

    public static String contents(UUID userId) {
        return CONTENTS_PREFIX + Objects.requireNonNull(userId, "userId must not be null");
    }

    public static String playlists(UUID userId) {
        return PLAYLISTS_PREFIX + Objects.requireNonNull(userId, "userId must not be null");
    }

    public static String refreshActivity(UUID userId) {
        return REFRESH_ACTIVITY_PREFIX + Objects.requireNonNull(userId, "userId must not be null");
    }

    public static String refreshLock(UUID userId) {
        return REFRESH_LOCK_PREFIX + Objects.requireNonNull(userId, "userId must not be null");
    }
}
