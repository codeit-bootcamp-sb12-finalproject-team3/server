package com.moduplaylist.realtime.watchparty.redis;

import java.util.Objects;
import java.util.UUID;

final class WatchPartyRedisKey {

    private static final String PREFIX = "watchparty:";

    private WatchPartyRedisKey() {
    }

    static String host(UUID partyId) {
        return PREFIX + require(partyId) + ":host";
    }

    static String joined(UUID partyId) {
        return PREFIX + require(partyId) + ":joined";
    }

    static String kicked(UUID partyId) {
        return PREFIX + require(partyId) + ":kicked";
    }

    static String online(UUID partyId) {
        return PREFIX + require(partyId) + ":online";
    }

    static String joinedParty(UUID userId) {
        return "user:" + require(userId) + ":joinedParty";
    }

    static String uuid(UUID id) {
        return require(id).toString();
    }

    static UUID parseUuid(String value) {
        return UUID.fromString(Objects.requireNonNull(value, "value must not be null"));
    }

    private static UUID require(UUID id) {
        return Objects.requireNonNull(id, "id must not be null");
    }
}
