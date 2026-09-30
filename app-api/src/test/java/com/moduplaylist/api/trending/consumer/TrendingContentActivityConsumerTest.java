package com.moduplaylist.api.trending.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.trending.policy.TrendingScorePolicy;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrendingContentActivityConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant OCCURRED_AT = Instant.parse("2026-09-23T02:15:00Z");

    @Mock
    private TrendingScorePolicy scorePolicy;

    @Mock
    private TrendingContentRedisRepository trendingRedisRepository;

    private TrendingContentActivityConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TrendingContentActivityConsumer(scorePolicy, trendingRedisRepository);
    }

    @Test
    void validActivityAppliesCalculatedScore() {
        ContentActivityKafkaEvent event = event(ContentActivityType.CONTENT_LIKE);
        when(scorePolicy.calculate(event)).thenReturn(1.5);

        consumer.consume(event);

        verify(trendingRedisRepository).applyScoreOnce(
                eq(EVENT_ID),
                eq(CONTENT_ID),
                eq(1.5),
                eq(OCCURRED_AT),
                any(Instant.class)
        );
    }

    @Test
    void zeroDeltaActivityDoesNotAccessRedis() {
        ContentActivityKafkaEvent event = event(ContentActivityType.CONTENT_RATING);
        when(scorePolicy.calculate(event)).thenReturn(0.0);

        consumer.consume(event);

        verify(trendingRedisRepository, never()).applyScoreOnce(
                any(), any(), eq(0.0), any(), any()
        );
    }

    @Test
    void activityMissingRequiredFieldIsIgnored() {
        ContentActivityKafkaEvent event = new ContentActivityKafkaEvent(
                EVENT_ID,
                ContentActivityType.CONTENT_VIEW,
                USER_ID,
                null,
                OCCURRED_AT
        );

        consumer.consume(event);

        verify(scorePolicy, never()).calculate(any());
        verify(trendingRedisRepository, never()).applyScoreOnce(
                any(), any(), any(Double.class), any(), any()
        );
    }

    private static ContentActivityKafkaEvent event(ContentActivityType type) {
        return new ContentActivityKafkaEvent(EVENT_ID, type, USER_ID, CONTENT_ID, OCCURRED_AT);
    }
}
