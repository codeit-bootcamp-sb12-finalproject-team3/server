package com.moduplaylist.infrastructure.redis.watchparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackState;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyPlaybackRegistry implements WatchPartyPlaybackRegistry {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void createOnLive(UUID partyId, WatchPartyPlaybackState state) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(state, "state가 필요합니다.");

        String key = WatchPartyRedisKey.playback(partyId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
        hashOps.putAll(key, toFieldMap(state));
        // LIVE 전이 시점엔 TTL을 걸지 않음 (설계 문서 참고 — ENDED 전이 시점에 별도로 armSafetyNetTtl 호출 예정)
    }

    @Override
    public Optional<WatchPartyPlaybackState> find(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");

        String key = WatchPartyRedisKey.playback(partyId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
        Map<String, Object> raw = hashOps.entries(key);

        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toState(raw));
    }

    private Map<String, Object> toFieldMap(WatchPartyPlaybackState state) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", state.getStatus());
        fields.put("startedAt", state.getStartedAt());
        fields.put("accumulatedPauseMs", state.getAccumulatedPauseMs());
        if (state.getStartEpisode() != null) {
            fields.put("startEpisode", state.getStartEpisode());
        }
        if (state.getEndEpisode() != null) {
            fields.put("endEpisode", state.getEndEpisode());
        }
        fields.put("hostId", state.getHostId());
        fields.put("updatedAt", state.getUpdatedAt());
        return fields;
    }

    private WatchPartyPlaybackState toState(Map<String, Object> raw) {
        return new WatchPartyPlaybackState(
                objectMapper.convertValue(raw.get("status"), WatchPartyPlaybackStatus.class),
                objectMapper.convertValue(raw.get("startedAt"), Long.class),
                objectMapper.convertValue(raw.get("accumulatedPauseMs"), Long.class),
                convertNullable(raw.get("startEpisode"), Integer.class),
                convertNullable(raw.get("endEpisode"), Integer.class),
                objectMapper.convertValue(raw.get("hostId"), UUID.class),
                objectMapper.convertValue(raw.get("updatedAt"), Long.class)
        );
    }

    private <T> T convertNullable(Object raw, Class<T> targetType) {
        return raw == null ? null : objectMapper.convertValue(raw, targetType);
    }
}