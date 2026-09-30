package com.moduplaylist.infrastructure.redis.watchparty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * app-api(발행) → app-realtime(수신) playback 채널 규격 중 "발행하는 쪽" 검증. (보내는 쪽)
 * realtime 쪽 WatchPartyPlaybackMessageListenerTest의 JSON과 필드명이 같아야 한다.
 */
class WatchPartyPlaybackMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 발행_JSON은_realtime과_같은_8개_필드를_담는다() throws Exception {
        UUID hostId = UUID.randomUUID();
        WatchPartyPlaybackState state = new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.LIVE, 1000L, 200L, 1500L, 1, 3, hostId, 2000L);

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(state));

        assertThat(node.size()).isEqualTo(8);
        assertThat(node.get("status").asText()).isEqualTo("LIVE");
        assertThat(node.get("startedAt").asLong()).isEqualTo(1000L);
        assertThat(node.get("accumulatedPauseMs").asLong()).isEqualTo(200L);
        assertThat(node.get("pausedAt").asLong()).isEqualTo(1500L);
        assertThat(node.get("startEpisode").asInt()).isEqualTo(1);
        assertThat(node.get("endEpisode").asInt()).isEqualTo(3);
        assertThat(node.get("hostId").asText()).isEqualTo(hostId.toString());
        assertThat(node.get("updatedAt").asLong()).isEqualTo(2000L);
    }

    @Test
    void 채널_이름이_realtime_구독_패턴과_맞는다() {
        UUID partyId = UUID.randomUUID();

        // realtime: PatternTopic("watchparty:*:playback")
        assertThat(WatchPartyRedisKey.playback(partyId))
                .isEqualTo("watchparty:" + partyId + ":playback");
    }
}