package com.moduplaylist.infrastructure.redis.watchparty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantBroadcaster;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantChangedMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWatchPartyParticipantBroadcaster implements WatchPartyParticipantBroadcaster {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void broadcast(UUID partyId, WatchPartyParticipantChangedMessage message) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(message, "message가 필요합니다.");

        String channel = WatchPartyRedisKey.participantsChannel(partyId);
        try {
            stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("Watch Party 참가자 변경 - Redis Pub/Sub 발행 실패. partyId={}", partyId, e);
        }
    }
}