package com.moduplaylist.realtime.watchparty.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.WatchPartyPlaybackRegistry;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackState;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyPlaybackRegistry implements WatchPartyPlaybackRegistry {

    static final String KEY_PREFIX = "watchparty:";
    static final String KEY_SUFFIX = ":playback";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisWatchPartyPlaybackRegistry(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WatchPartyPlaybackState> find(UUID partyId) {
        String key = KEY_PREFIX + partyId + KEY_SUFFIX;
        Map<String, String> raw = redisTemplate.<String, String>opsForHash().entries(key);

        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toState(raw));
    }

    @Override
    public void update(UUID partyId, WatchPartyPlaybackState state) {
        String key = KEY_PREFIX + partyId + KEY_SUFFIX;
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        Map<String, String> fields = new HashMap<>();
        fields.put("status", serialize(state.getStatus()));
        fields.put("startedAt", serialize(state.getStartedAt()));
        fields.put("accumulatedPauseMs", serialize(state.getAccumulatedPauseMs()));
        fields.put("hostId", serialize(state.getHostId()));
        fields.put("updatedAt", serialize(state.getUpdatedAt()));
        hashOps.putAll(key, fields);

        // nullable 필드는 값이 있으면 쓰고, null이면 명시적으로 지운다 (낡은 값이 남지 않도록)
        putOrClear(hashOps, key, "pausedAt", state.getPausedAt());
        putOrClear(hashOps, key, "startEpisode", state.getStartEpisode());
        putOrClear(hashOps, key, "endEpisode", state.getEndEpisode());
    }

    private void putOrClear(HashOperations<String, String, String> hashOps, String key, String field, Object value) {
        if (value == null) {
            hashOps.delete(key, field);
        } else {
            hashOps.put(key, field, serialize(value));
        }
    }

    private WatchPartyPlaybackState toState(Map<String, String> raw) {
        return new WatchPartyPlaybackState(
                deserialize(raw.get("status"), WatchPartyPlaybackStatus.class),
                deserialize(raw.get("startedAt"), Long.class),
                deserialize(raw.get("accumulatedPauseMs"), Long.class),
                deserialize(raw.get("pausedAt"), Long.class),
                deserialize(raw.get("startEpisode"), Integer.class),
                deserialize(raw.get("endEpisode"), Integer.class),
                deserialize(raw.get("hostId"), UUID.class),
                deserialize(raw.get("updatedAt"), Long.class)
        );
    }

    private <T> T deserialize(String raw, Class<T> type) {
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Playback 상태 역직렬화에 실패했습니다.", e);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Playback 상태 직렬화에 실패했습니다.", e);
        }
    }
}