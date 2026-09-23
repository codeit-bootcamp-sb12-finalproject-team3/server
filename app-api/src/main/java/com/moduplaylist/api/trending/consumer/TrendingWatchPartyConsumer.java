package com.moduplaylist.api.trending.consumer;

import com.moduplaylist.api.trending.policy.TrendingScorePolicy;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyParticipantJoinedKafkaEvent;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import com.moduplaylist.infrastructure.trending.TrendingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendingWatchPartyConsumer {

    private final TrendingScorePolicy scorePolicy;
    private final TrendingContentRedisRepository trendingRedisRepository;
    private final TrendingProperties properties;

    @KafkaListener(
            topics = KafkaTopics.WATCH_PARTY_PARTICIPANT_CHANGED,
            groupId = "trending-watch-party-score"
    )
    public void consume(WatchPartyParticipantJoinedKafkaEvent event) {
        if (!isValid(event)) {
            log.warn("유효하지 않은 같이보기 최초 참가 이벤트를 무시합니다. event={}", event);
            return;
        }

        trendingRedisRepository.applyScoreOnce(
                event.eventId(),
                event.contentId(),
                scorePolicy.watchPartyParticipation(),
                properties.getProcessedEventTtl()
        );
    }

    private boolean isValid(WatchPartyParticipantJoinedKafkaEvent event) {
        return event != null
                && event.eventId() != null
                && event.watchPartyId() != null
                && event.userId() != null
                && event.contentId() != null
                && event.occurredAt() != null;
    }
}
