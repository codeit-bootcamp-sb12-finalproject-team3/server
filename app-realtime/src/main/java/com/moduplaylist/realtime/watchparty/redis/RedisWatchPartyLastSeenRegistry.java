package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyLastSeenRegistry;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyLastSeenRegistry implements WatchPartyLastSeenRegistry {

    // 파티 종료로 키가 삭제된 뒤에도, 아직 구독 중인 사람의 하트비트가 키를 다시 만들 수 있다. (안전망)
    // TTL로 아무도 갱신하지 않는 키는 결국 사라지게 한다.
    // 단, 배포 등으로 전원이 잠깐 끊겨도 기록이 남아 있어야 하므로 유령 기준(5분)보다 훨씬 길게 둔다.
    private static final Duration KEY_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyLastSeenRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void touch(UUID partyId, UUID userId, long seenAtMillis) {
        String key = WatchPartyRedisKey.lastSeen(partyId);
        redisTemplate.opsForHash().put(key, WatchPartyRedisKey.uuid(userId), String.valueOf(seenAtMillis));
        redisTemplate.expire(key, KEY_TTL);
    }

    @Override
    public void touchAll(Map<UUID, Set<UUID>> userIdsByPartyId, long seenAtMillis) {
        String seenAt = String.valueOf(seenAtMillis);
        userIdsByPartyId.forEach((partyId, userIds) -> {
            Map<String, String> fields = new HashMap<>();
            for (UUID userId : userIds) {
                fields.put(WatchPartyRedisKey.uuid(userId), seenAt);
            }
            String key = WatchPartyRedisKey.lastSeen(partyId);
            redisTemplate.opsForHash().putAll(key, fields);
            redisTemplate.expire(key, KEY_TTL);
        });
    }
}