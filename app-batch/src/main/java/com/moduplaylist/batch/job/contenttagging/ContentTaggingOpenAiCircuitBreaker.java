package com.moduplaylist.batch.job.contenttagging;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.batch.content-tagging", name = "enabled", havingValue = "true")
public class ContentTaggingOpenAiCircuitBreaker {

    private static final String CIRCUIT_KEY = "mopl:content-tagging:openai-circuit";
    private static final String PROBE_KEY = CIRCUIT_KEY + ":probe";
    private static final Duration INITIAL_OPEN = Duration.ofMinutes(10);
    private static final Duration SECOND_OPEN = Duration.ofMinutes(10);
    private static final Duration THIRD_OPEN = Duration.ofHours(12);
    private static final Duration PROBE_LEASE = Duration.ofMinutes(5);

    private static final DefaultRedisScript<String> ACQUIRE_SCRIPT = new DefaultRedisScript<>("""
        local state = redis.call('HGET', KEYS[1], 'state') or 'CLOSED'
        local stage = tonumber(redis.call('HGET', KEYS[1], 'stage') or '-1')
        if state == 'CLOSED' then
            return 'NORMAL'
        end
        if state == 'LONG_OPEN' or state == 'OPEN' then
            local openUntil = tonumber(redis.call('HGET', KEYS[1], 'openUntil') or '0')
            if tonumber(ARGV[1]) < openUntil then
                return 'DENIED'
            end
        end
        local claimed = redis.call('SET', KEYS[2], ARGV[2], 'NX', 'PX', ARGV[3])
        if not claimed then
            return 'DENIED'
        end
        redis.call('HSET', KEYS[1], 'state', 'HALF_OPEN')
        return 'PROBE'
        """, String.class);

    private static final DefaultRedisScript<Long> OPEN_SCRIPT = new DefaultRedisScript<>("""
        local state = redis.call('HGET', KEYS[1], 'state') or 'CLOSED'
        if state ~= 'CLOSED' then
            return 0
        end
        if ARGV[1] == 'true' then
            redis.call('HSET', KEYS[1], 'state', 'LONG_OPEN', 'stage', '3', 'openUntil', ARGV[3])
        else
            redis.call('HSET', KEYS[1], 'state', 'OPEN', 'stage', '0', 'openUntil', ARGV[2])
        end
        return 1
        """, Long.class);

    private static final DefaultRedisScript<Long> PROBE_SUCCESS_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[2]) ~= ARGV[1] then
            return 0
        end
        redis.call('DEL', KEYS[1])
        redis.call('DEL', KEYS[2])
        return 1
        """, Long.class);

    private static final DefaultRedisScript<Long> PROBE_FAILURE_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[2]) ~= ARGV[1] then
            return 0
        end
        local stage = tonumber(redis.call('HGET', KEYS[1], 'stage') or '0')
        if ARGV[2] == 'true' or stage >= 2 then
            redis.call('HSET', KEYS[1], 'state', 'LONG_OPEN', 'stage', '3', 'openUntil', ARGV[4])
        elseif stage == 0 then
            redis.call('HSET', KEYS[1], 'state', 'OPEN', 'stage', '1', 'openUntil', ARGV[3])
        else
            redis.call('HSET', KEYS[1], 'state', 'OPEN', 'stage', '2', 'openUntil', ARGV[4])
        end
        redis.call('DEL', KEYS[2])
        return 1
        """, Long.class);

    private static final DefaultRedisScript<Long> PROBE_ABORT_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[2]) ~= ARGV[1] then
            return 0
        end
        local stage = tonumber(redis.call('HGET', KEYS[1], 'stage') or '0')
        if stage >= 3 then
            redis.call('HSET', KEYS[1], 'state', 'LONG_OPEN', 'stage', '3', 'openUntil', ARGV[4])
        elseif stage == 0 then
            redis.call('HSET', KEYS[1], 'state', 'OPEN', 'openUntil', ARGV[2])
        elseif stage == 1 then
            redis.call('HSET', KEYS[1], 'state', 'OPEN', 'openUntil', ARGV[3])
        else
            redis.call('HSET', KEYS[1], 'state', 'OPEN', 'openUntil', ARGV[4])
        end
        redis.call('DEL', KEYS[2])
        return 1
        """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public Permit acquire() {
        String token = UUID.randomUUID().toString();
        String result = redisTemplate.execute(
                ACQUIRE_SCRIPT,
                List.of(CIRCUIT_KEY, PROBE_KEY),
                Long.toString(Instant.now().toEpochMilli()),
                token,
                Long.toString(PROBE_LEASE.toMillis())
        );
        if ("NORMAL".equals(result)) return Permit.normal();
        if ("PROBE".equals(result)) return Permit.probe(token);
        return Permit.denied();
    }

    public void openAfterConsecutiveFailures(boolean permanent) {
        Instant now = Instant.now();
        redisTemplate.execute(
                OPEN_SCRIPT,
                List.of(CIRCUIT_KEY),
                Boolean.toString(permanent),
                Long.toString(now.plus(INITIAL_OPEN).toEpochMilli()),
                Long.toString(now.plus(THIRD_OPEN).toEpochMilli())
        );
    }

    public void probeSucceeded(String token) {
        redisTemplate.execute(PROBE_SUCCESS_SCRIPT, List.of(CIRCUIT_KEY, PROBE_KEY), token);
    }

    public void probeFailed(String token, boolean permanent) {
        Instant now = Instant.now();
        redisTemplate.execute(
                PROBE_FAILURE_SCRIPT,
                List.of(CIRCUIT_KEY, PROBE_KEY),
                token,
                Boolean.toString(permanent),
                Long.toString(now.plus(SECOND_OPEN).toEpochMilli()),
                Long.toString(now.plus(THIRD_OPEN).toEpochMilli())
        );
    }

    public void probeAborted(String token) {
        Instant now = Instant.now();
        redisTemplate.execute(
                PROBE_ABORT_SCRIPT,
                List.of(CIRCUIT_KEY, PROBE_KEY),
                token,
                Long.toString(now.plus(INITIAL_OPEN).toEpochMilli()),
                Long.toString(now.plus(SECOND_OPEN).toEpochMilli()),
                Long.toString(now.plus(THIRD_OPEN).toEpochMilli())
        );
    }

    public record Permit(boolean allowed, boolean probe, String token) {
        private static Permit normal() { return new Permit(true, false, null); }
        private static Permit probe(String token) { return new Permit(true, true, token); }
        private static Permit denied() { return new Permit(false, false, null); }
    }
}
