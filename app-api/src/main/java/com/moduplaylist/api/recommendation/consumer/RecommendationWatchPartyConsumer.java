package com.moduplaylist.api.recommendation.consumer;

import com.moduplaylist.api.recommendation.service.ContentPreferenceUpdateService;
import com.moduplaylist.api.recommendation.event.PreferenceChangedEvent;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.recommendation.repository.RecommendationProcessedEventRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyParticipantJoinedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationWatchPartyConsumer {

    private final ContentPreferenceUpdateService contentPreferenceUpdateService;
    private final RecommendationProcessedEventRepository processedEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @KafkaListener(
            topics = KafkaTopics.WATCH_PARTY_PARTICIPANT_CHANGED,
            groupId = "recommendation-watch-party-preference"
    )
    public void consume(WatchPartyParticipantJoinedKafkaEvent event) {
        if (event.eventId() == null
                || event.watchPartyId() == null
                || event.userId() == null
                || event.contentId() == null) {
            log.warn("유효하지 않은 WatchParty 최초 참여 이벤트를 무시합니다. eventId={}", event.eventId());
            return;
        }
        if (!processedEventRepository.tryMarkProcessed(event.eventId())) {
            return;
        }

        double appliedDelta = contentPreferenceUpdateService.applyActivity(
                event.userId(),
                event.contentId(),
                ContentActivityType.WATCH_PARTY_JOINED
        );
        if (appliedDelta != 0.0) {
            eventPublisher.publishEvent(new PreferenceChangedEvent(
                    event.eventId(),
                    event.userId(),
                    appliedDelta
            ));
        }
    }
}
