package com.moduplaylist.infrastructure.redis.trending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import com.moduplaylist.infrastructure.trending.TrendingProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TESTS", matches = "true")
class TrendingContentRedisRepositoryIntegrationTest {

    private static final int TEST_DATABASE = 15;
    private static final UUID CONTENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_CONTENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private TrendingContentRedisRepository repository;
    private TrendingProperties properties;

    @BeforeAll
    static void connectRedis() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.setDatabase(TEST_DATABASE);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void disconnectRedis() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        properties = new TrendingProperties();
        repository = new TrendingContentRedisRepository(redisTemplate, properties);
    }

    @AfterEach
    void clearTestDatabase() {
        connectionFactory.getConnection().serverCommands().flushDb();
    }

    @Test
    void duplicateEventIsAppliedOnceWithProcessedEventAndBucketTtls() {
        Instant now = Instant.now();
        Instant occurredAt = now.minus(10, ChronoUnit.MINUTES);
        UUID eventId = UUID.fromString("20000000-0000-0000-0000-000000000001");

        boolean first = repository.applyScoreOnce(eventId, CONTENT_ID, 1.5, occurredAt, now);
        boolean duplicate = repository.applyScoreOnce(eventId, CONTENT_ID, 1.5, occurredAt, now);

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(redisTemplate.opsForZSet().score(
                TrendingRedisKey.contentsBucket(occurredAt),
                CONTENT_ID.toString()
        )).isCloseTo(1.5, offset(0.000_001));

        Duration processedEventTtl = Duration.ofMillis(redisTemplate.getExpire(
                TrendingRedisKey.processedEvent(eventId),
                TimeUnit.MILLISECONDS
        ));
        Duration bucketTtl = Duration.ofMillis(redisTemplate.getExpire(
                TrendingRedisKey.contentsBucket(occurredAt),
                TimeUnit.MILLISECONDS
        ));
        assertThat(processedEventTtl).isPositive().isLessThanOrEqualTo(properties.getProcessedEventTtl());
        assertThat(bucketTtl).isGreaterThan(Duration.ofHours(24))
                .isLessThanOrEqualTo(properties.getBucketTtl());
    }

    @Test
    void twentyFourHourBucketsAreAggregatedAndNonPositiveScoresAreExcluded() {
        Instant now = Instant.now();
        repository.applyScoreOnce(
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                CONTENT_ID,
                1.5,
                now.minus(10, ChronoUnit.MINUTES),
                now
        );
        repository.applyScoreOnce(
                UUID.fromString("20000000-0000-0000-0000-000000000003"),
                CONTENT_ID,
                -0.1,
                now.minus(70, ChronoUnit.MINUTES),
                now
        );
        repository.applyScoreOnce(
                UUID.fromString("20000000-0000-0000-0000-000000000004"),
                OTHER_CONTENT_ID,
                -1.0,
                now.minus(10, ChronoUnit.MINUTES),
                now
        );

        assertThat(repository.findTopContentIds(10, now)).containsExactly(CONTENT_ID);
        assertThat(redisTemplate.opsForZSet().score(
                TrendingRedisKey.aggregate(now),
                CONTENT_ID.toString()
        )).isCloseTo(1.4, offset(0.000_001));
    }

    @Test
    void eventOutsideActiveWindowIsNotRecorded() {
        Instant now = Instant.now();
        Instant occurredAt = TrendingRedisKey.bucketStart(now).minus(24, ChronoUnit.HOURS);
        UUID eventId = UUID.fromString("20000000-0000-0000-0000-000000000005");

        boolean applied = repository.applyScoreOnce(eventId, CONTENT_ID, 1.0, occurredAt, now);

        assertThat(applied).isFalse();
        assertThat(redisTemplate.hasKey(TrendingRedisKey.processedEvent(eventId))).isFalse();
        assertThat(redisTemplate.hasKey(TrendingRedisKey.contentsBucket(occurredAt))).isFalse();
    }

    @Test
    void contentDeletionRemovesScoresFromBucketsAndAggregate() {
        Instant now = Instant.now();
        Instant currentEventTime = now.minus(10, ChronoUnit.MINUTES);
        Instant previousEventTime = now.minus(70, ChronoUnit.MINUTES);
        repository.applyScoreOnce(
                UUID.fromString("20000000-0000-0000-0000-000000000006"),
                CONTENT_ID,
                1.0,
                currentEventTime,
                now
        );
        repository.applyScoreOnce(
                UUID.fromString("20000000-0000-0000-0000-000000000007"),
                CONTENT_ID,
                1.0,
                previousEventTime,
                now
        );
        repository.findTopContentIds(10, now);

        repository.remove(CONTENT_ID, now);

        assertThat(redisTemplate.opsForZSet().score(
                TrendingRedisKey.contentsBucket(currentEventTime),
                CONTENT_ID.toString()
        )).isNull();
        assertThat(redisTemplate.opsForZSet().score(
                TrendingRedisKey.contentsBucket(previousEventTime),
                CONTENT_ID.toString()
        )).isNull();
        assertThat(redisTemplate.opsForZSet().score(
                TrendingRedisKey.aggregate(now),
                CONTENT_ID.toString()
        )).isNull();
    }

    @Test
    void luaFailureDoesNotLeaveProcessedEventWithoutScore() {
        Instant now = Instant.now();
        Instant occurredAt = now.minus(10, ChronoUnit.MINUTES);
        UUID eventId = UUID.fromString("20000000-0000-0000-0000-000000000008");
        String bucketKey = TrendingRedisKey.contentsBucket(occurredAt);
        redisTemplate.opsForValue().set(bucketKey, "wrong-type");

        assertThatThrownBy(() -> repository.applyScoreOnce(
                eventId,
                CONTENT_ID,
                1.0,
                occurredAt,
                now
        )).isInstanceOf(RuntimeException.class);

        assertThat(redisTemplate.hasKey(TrendingRedisKey.processedEvent(eventId))).isFalse();
        assertThat(redisTemplate.opsForValue().get(bucketKey)).isEqualTo("wrong-type");
    }
}
