package com.moduplaylist.infrastructure.redis.watchparty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackBroadcaster;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackState;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWatchPartyPlaybackBroadcaster implements WatchPartyPlaybackBroadcaster {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void broadcastStarted(UUID partyId, WatchPartyPlaybackState state) {
        publish(partyId, state, "시작");
    }


    @Override
    public void broadcastEnded(UUID partyId, WatchPartyPlaybackState state) {
        publish(partyId, state, "종료");
    }

    // 시작/종료 공통: 같은 playback 채널에 상태 JSON을 그대로 발행
    private void publish(UUID partyId, WatchPartyPlaybackState state, String action) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(state, "state가 필요합니다.");

        String channel = WatchPartyRedisKey.playback(partyId);
        try {
            stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            log.error("Watch Party {} - Redis Pub/Sub 발행 실패. partyId={}", action, partyId, e);
        }
    }
}