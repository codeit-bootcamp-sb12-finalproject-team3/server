package com.moduplaylist.infrastructure.redis.lock;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DistributedLockRegistry {

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "  return redis.call('del', KEYS[1]) "
                    + "else return 0 end";

    private final RedisTemplate<String, Object> redisTemplate;

    /** 락 획득 성공 시 해제할 때 쓸 토큰을 반환, 실패하면 null */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    /** 내가 건 락일 때만 안전하게 해제 (Lua로 원자적 처리) */
    public void unlock(String key, String token) {
        redisTemplate.execute(
                new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class),
                List.of(key), token);
    }
}