package com.moduplaylist.api.recommendation.consumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.recommendation.event.PreferenceChangedEvent;
import com.moduplaylist.api.recommendation.service.ContentPreferenceUpdateService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.recommendation.repository.RecommendationProcessedEventRepository;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RecommendationContentActivityConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private ContentPreferenceUpdateService contentPreferenceUpdateService;

    @Mock
    private RecommendationProcessedEventRepository processedEventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RecommendationContentActivityConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RecommendationContentActivityConsumer(
                contentPreferenceUpdateService,
                processedEventRepository,
                eventPublisher
        );
    }

    @Test
    void fiveStarRatingPublishesAppliedRecommendationWeight() {
        ContentActivityKafkaEvent event = new ContentActivityKafkaEvent(
                EVENT_ID,
                ContentActivityType.CONTENT_RATING,
                USER_ID,
                CONTENT_ID,
                null,
                new BigDecimal("5.0"),
                Instant.parse("2026-09-26T07:09:00Z")
        );
        when(processedEventRepository.tryMarkProcessed(EVENT_ID)).thenReturn(true);
        when(contentPreferenceUpdateService.applyRatingCreated(USER_ID, CONTENT_ID, 5.0))
                .thenReturn(1.0);

        consumer.consume(event);

        ArgumentCaptor<PreferenceChangedEvent> captor =
                ArgumentCaptor.forClass(PreferenceChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PreferenceChangedEvent published = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(EVENT_ID, published.eventId());
        org.junit.jupiter.api.Assertions.assertEquals(USER_ID, published.userId());
        org.junit.jupiter.api.Assertions.assertEquals(1.0, published.appliedDelta());
    }

    @Test
    void unsupportedContentViewIsIgnoredBeforeMarkingProcessed() {
        ContentActivityKafkaEvent event = new ContentActivityKafkaEvent(
                EVENT_ID,
                ContentActivityType.CONTENT_VIEW,
                USER_ID,
                CONTENT_ID,
                Instant.parse("2026-09-26T07:09:00Z")
        );

        consumer.consume(event);

        verify(processedEventRepository, never()).tryMarkProcessed(EVENT_ID);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
