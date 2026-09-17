package com.moduplaylist.infrastructure.redis.watchparty;

import java.util.Objects;
import java.util.UUID;

public final class WatchPartyRedisKey {

    private static final String PREFIX = "watchparty:";

    private WatchPartyRedisKey() {
    }

    public static String playback(UUID partyId) {
        return PREFIX + require(partyId) + ":playback";
    }

    public static String online(UUID partyId) {
        return PREFIX + require(partyId) + ":online";
    }

    public static String kicked(UUID partyId) {
        return PREFIX + require(partyId) + ":kicked";
    }

    public static String chatLog(UUID partyId) {
        return PREFIX + require(partyId) + ":chat:log";
    }

    public static String chatChannel(UUID partyId) {
        return PREFIX + require(partyId) + ":chat";
    }

    private static UUID require(UUID partyId) {
        return Objects.requireNonNull(partyId, "partyId must not be null");
    }
}