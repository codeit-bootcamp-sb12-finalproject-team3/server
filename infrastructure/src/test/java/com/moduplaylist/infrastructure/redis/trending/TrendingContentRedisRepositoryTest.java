package com.moduplaylist.infrastructure.redis.trending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.infrastructure.trending.TrendingProperties;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class TrendingContentRedisRepositoryTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private TrendingContentRedisRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TrendingContentRedisRepository(
                redisTemplate,
                new TrendingProperties()
        );
    }

    @Test
    void applyScoreOnceUsesOccurredAtBucketAndFixedBucketExpiration() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");
        Instant occurredAt = Instant.parse("2026-09-23T01:45:00Z");
        Instant bucketExpiresAt = Instant.parse("2026-09-24T03:00:00Z");
        doReturn(1L).when(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any()
        );

        boolean applied = repository.applyScoreOnce(
                EVENT_ID,
                CONTENT_ID,
                0.1,
                occurredAt,
                now
        );

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(
                any(RedisScript.class),
                keysCaptor.capture(),
                eq("604800000"),
                eq("0.1"),
                eq(CONTENT_ID.toString()),
                eq(Long.toString(bucketExpiresAt.toEpochMilli()))
        );
        assertThat(keysCaptor.getValue()).containsExactly(
                TrendingRedisKey.processedEvent(EVENT_ID),
                "trending:contents:2026092301"
        );
        assertThat(applied).isTrue();
    }

    @Test
    void applyScoreOnceIgnoresEventOutsideActiveWindow() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");
        Instant occurredAt = Instant.parse("2026-09-22T02:59:59Z");

        boolean applied = repository.applyScoreOnce(
                EVENT_ID,
                CONTENT_ID,
                0.1,
                occurredAt,
                now
        );

        assertThat(applied).isFalse();
        verify(redisTemplate, never()).execute(
                any(RedisScript.class),
                anyList(),
                any()
        );
    }

    @Test
    void findTopContentIdsAggregatesTwentyFourBucketsAndExcludesNonPositiveScores() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");
        UUID positiveContentId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID zeroContentId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID negativeContentId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        Set<ZSetOperations.TypedTuple<String>> values = new LinkedHashSet<>(List.of(
                new DefaultTypedTuple<>(positiveContentId.toString(), 2.0),
                new DefaultTypedTuple<>(zeroContentId.toString(), 0.0),
                new DefaultTypedTuple<>(negativeContentId.toString(), -1.0)
        ));
        doReturn(1L).when(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any()
        );
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(
                TrendingRedisKey.aggregate(now),
                0,
                9
        )).thenReturn(values);

        List<UUID> result = repository.findTopContentIds(10, now);

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(
                any(RedisScript.class),
                keysCaptor.capture(),
                eq("30000")
        );
        assertThat(keysCaptor.getValue())
                .hasSize(26)
                .startsWith(
                        "trending:contents:aggregate:2026092302",
                        "trending:contents:aggregate:2026092302:ready",
                        "trending:contents:2026092302"
                )
                .endsWith("trending:contents:2026092203");
        assertThat(result).containsExactly(positiveContentId);
    }

    @Test
    void removeDeletesContentFromActiveBucketsAggregateAndLegacyKey() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");
        doReturn(26L).when(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any()
        );

        repository.remove(CONTENT_ID, now);

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(
                any(RedisScript.class),
                keysCaptor.capture(),
                eq(CONTENT_ID.toString())
        );
        assertThat(keysCaptor.getValue())
                .hasSize(26)
                .contains(
                        "trending:contents:2026092302",
                        "trending:contents:2026092203",
                        "trending:contents:aggregate:2026092302",
                        TrendingRedisKey.CONTENTS
                );
    }
}
