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

    public static String host(UUID partyId) {
        return PREFIX + require(partyId) + ":host";
    }

    public static String joined(UUID partyId) {
        return PREFIX + require(partyId) + ":joined";
    }

    // 파티 기준이 아니라 유저 기준이라 prefix가 다름 (watchparty: 아니라 user:)
    public static String joinedParty(UUID userId) {
        return "user:" + require(userId) + ":joinedParty";
    }


    private static UUID require(UUID id) {
        return Objects.requireNonNull(id, "id must not be null");
    }
}