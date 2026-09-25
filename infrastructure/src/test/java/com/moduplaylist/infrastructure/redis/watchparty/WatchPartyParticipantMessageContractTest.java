package com.moduplaylist.infrastructure.redis.watchparty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantChangedMessage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * app-api(발행) → app-realtime(수신) 참가자 채널 규격 중 "발행하는 쪽" 검증.
 * realtime 쪽 WatchPartyParticipantMessageListenerTest의 JSON과 필드명이 같아야 한다.
 */
class WatchPartyParticipantMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 발행_JSON은_userId와_status_문자열만_담는다() throws Exception {
        UUID userId = UUID.randomUUID();

        String json = objectMapper.writeValueAsString(
                new WatchPartyParticipantChangedMessage(userId, ParticipantStatus.KICKED));
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.size()).isEqualTo(2);
        assertThat(node.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(node.get("status").asText()).isEqualTo("KICKED");
    }

    @Test
    void 채널_이름이_realtime_구독_패턴과_맞는다() {
        UUID partyId = UUID.randomUUID();

        // realtime: PatternTopic("watchparty:*:participants")
        assertThat(WatchPartyRedisKey.participantsChannel(partyId))
                .isEqualTo("watchparty:" + partyId + ":participants");
    }
}