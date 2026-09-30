package com.moduplaylist.api.trending.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.trending.policy.TrendingScorePolicy;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyParticipantJoinedKafkaEvent;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrendingWatchPartyConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PARTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final Instant OCCURRED_AT = Instant.parse("2026-09-23T02:15:00Z");

    @Mock
    private TrendingScorePolicy scorePolicy;

    @Mock
    private TrendingContentRedisRepository trendingRedisRepository;

    private TrendingWatchPartyConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TrendingWatchPartyConsumer(scorePolicy, trendingRedisRepository);
    }

    @Test
    void validFirstParticipationAppliesConfiguredScore() {
        WatchPartyParticipantJoinedKafkaEvent event = new WatchPartyParticipantJoinedKafkaEvent(
                EVENT_ID, PARTY_ID, USER_ID, CONTENT_ID, OCCURRED_AT
        );
        when(scorePolicy.watchPartyParticipation()).thenReturn(1.0);

        consumer.consume(event);

        verify(trendingRedisRepository).applyScoreOnce(
                eq(EVENT_ID),
                eq(CONTENT_ID),
                eq(1.0),
                eq(OCCURRED_AT),
                any(Instant.class)
        );
    }

    @Test
    void participationMissingRequiredFieldIsIgnored() {
        WatchPartyParticipantJoinedKafkaEvent event = new WatchPartyParticipantJoinedKafkaEvent(
                EVENT_ID, PARTY_ID, USER_ID, null, OCCURRED_AT
        );

        consumer.consume(event);

        verify(scorePolicy, never()).watchPartyParticipation();
        verify(trendingRedisRepository, never()).applyScoreOnce(
                any(), any(), any(Double.class), any(), any()
        );
    }
}
