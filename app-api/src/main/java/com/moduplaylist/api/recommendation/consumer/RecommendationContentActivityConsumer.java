package com.moduplaylist.api.recommendation.consumer;

import com.moduplaylist.api.recommendation.event.PreferenceChangedEvent;
import com.moduplaylist.api.recommendation.service.ContentPreferenceUpdateService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.recommendation.repository.RecommendationProcessedEventRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationContentActivityConsumer {

    private final ContentPreferenceUpdateService contentPreferenceUpdateService;
    private final RecommendationProcessedEventRepository processedEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @KafkaListener(
            topics = KafkaTopics.CONTENT_ACTIVITIES,
            groupId = "recommendation-content-preference"
    )
    public void consume(Object payload) {
        if (!(payload instanceof ContentActivityKafkaEvent event)) {
            return;
        }
        if (event.eventId() == null
                || event.eventType() == null
                || event.userId() == null
                || event.contentId() == null) {
            log.warn("유효하지 않은 콘텐츠 활동 이벤트를 무시합니다. eventId={}", event.eventId());
            return;
        }
        if (!isSupported(event.eventType())
                || !processedEventRepository.tryMarkProcessed(event.eventId())) {
            return;
        }

        double appliedDelta = switch (event.eventType()) {
            case CONTENT_LIKE,
                    CONTENT_UNLIKE,
                    PLAYLIST_CONTENT_ADDED,
                    PLAYLIST_CONTENT_REMOVED -> contentPreferenceUpdateService.applyActivity(
                            event.userId(),
                            event.contentId(),
                            event.eventType()
                    );
            case CONTENT_RATING -> applyRating(event);
            case INITIAL_PREFERENCE, WATCH_PARTY_JOINED, CONTENT_VIEW -> {
                // 이 토픽의 Recommendation Consumer가 처리하지 않는 이벤트
                yield 0.0;
            }
        };
        if (appliedDelta != 0.0) {
            eventPublisher.publishEvent(new PreferenceChangedEvent(
                    event.eventId(),
                    event.userId(),
                    appliedDelta
            ));
        }
    }

    private boolean isSupported(ContentActivityType eventType) {
        return switch (eventType) {
            case CONTENT_LIKE,
                    CONTENT_UNLIKE,
                    CONTENT_RATING,
                    PLAYLIST_CONTENT_ADDED,
                    PLAYLIST_CONTENT_REMOVED -> true;
            case INITIAL_PREFERENCE, WATCH_PARTY_JOINED, CONTENT_VIEW -> false;
        };
    }

    private double applyRating(ContentActivityKafkaEvent event) {
        BigDecimal oldRating = event.oldRating();
        BigDecimal newRating = event.newRating();

        if (oldRating == null && newRating != null) {
            return contentPreferenceUpdateService.applyRatingCreated(
                    event.userId(),
                    event.contentId(),
                    newRating.doubleValue()
            );
        }
        if (oldRating != null && newRating != null) {
            return contentPreferenceUpdateService.applyRatingChanged(
                    event.userId(),
                    event.contentId(),
                    oldRating.doubleValue(),
                    newRating.doubleValue()
            );
        }
        if (oldRating != null) {
            return contentPreferenceUpdateService.applyRatingDeleted(
                    event.userId(),
                    event.contentId(),
                    oldRating.doubleValue()
            );
        }

        log.warn("평점 값이 없는 콘텐츠 평점 이벤트를 무시합니다. eventId={}", event.eventId());
        return 0.0;
    }
}
