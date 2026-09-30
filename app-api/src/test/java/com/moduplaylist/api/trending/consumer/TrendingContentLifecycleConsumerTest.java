package com.moduplaylist.api.trending.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.moduplaylist.infrastructure.kafka.event.ContentDeleted;
import com.moduplaylist.infrastructure.kafka.event.ContentUpserted;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrendingContentLifecycleConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private TrendingContentRedisRepository trendingRedisRepository;

    private TrendingContentLifecycleConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TrendingContentLifecycleConsumer(trendingRedisRepository);
    }

    @Test
    void contentDeletedRemovesContentFromTrending() {
        consumer.consume(record(new ContentDeleted(
                EVENT_ID,
                CONTENT_ID,
                Instant.parse("2026-09-23T02:15:00Z")
        )));

        verify(trendingRedisRepository).remove(eq(CONTENT_ID), any(Instant.class));
    }

    @Test
    void unsupportedPayloadIsIgnored() {
        consumer.consume(record("unsupported"));

        verify(trendingRedisRepository, never()).remove(any(), any());
    }

    @Test
    void contentUpsertedIsIgnored() {
        consumer.consume(record(new ContentUpserted(
                EVENT_ID,
                CONTENT_ID,
                Instant.parse("2026-09-23T02:15:00Z")
        )));

        verify(trendingRedisRepository, never()).remove(any(), any());
    }

    @Test
    void deletionMissingContentIdIsIgnored() {
        consumer.consume(record(new ContentDeleted(
                EVENT_ID,
                null,
                Instant.parse("2026-09-23T02:15:00Z")
        )));

        verify(trendingRedisRepository, never()).remove(any(), any());
    }

    private static ConsumerRecord<String, Object> record(Object payload) {
        return new ConsumerRecord<>("content-lifecycle", 0, 0L, null, payload);
    }
}
