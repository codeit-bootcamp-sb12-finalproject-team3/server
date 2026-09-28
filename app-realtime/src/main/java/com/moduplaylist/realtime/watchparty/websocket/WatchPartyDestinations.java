package com.moduplaylist.realtime.watchparty.websocket;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 구독 경로·Redis 채널에서 partyId/UUID를 꺼낸다 (하트비트, SUBSCRIBE 리스너, playback 종료 처리)
public final class WatchPartyDestinations {

    private static final Pattern PARTY_SUBSCRIPTION_PATTERN =
            Pattern.compile("^/sub/watch-parties/([^/]+)/.*$");

    private WatchPartyDestinations() {
    }

    // 파티 구독 경로가 아니거나 UUID 형식이 아니면 null
    static UUID parsePartyId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = PARTY_SUBSCRIPTION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        return parseUuid(matcher.group(1));
    }

    public static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return null;
        }
    }
}