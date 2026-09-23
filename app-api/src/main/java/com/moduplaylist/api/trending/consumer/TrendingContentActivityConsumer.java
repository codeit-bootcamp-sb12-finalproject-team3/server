package com.moduplaylist.api.trending.consumer;

import com.moduplaylist.api.trending.policy.TrendingScorePolicy;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import com.moduplaylist.infrastructure.trending.TrendingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendingContentActivityConsumer {

    private final TrendingScorePolicy scorePolicy;
    private final TrendingContentRedisRepository trendingRedisRepository;
    private final TrendingProperties properties;

    @KafkaListener(
            topics = KafkaTopics.CONTENT_ACTIVITIES,
            groupId = "trending-content-score"
    )
    public void consume(ContentActivityKafkaEvent event) {
        if (!isValid(event)) {
            log.warn("유효하지 않은 트렌딩 콘텐츠 활동 이벤트를 무시합니다. event={}", event);
            return;
        }

        double delta = scorePolicy.calculate(event);
        if (delta == 0.0) {
            return;
        }

        trendingRedisRepository.applyScoreOnce(
                event.eventId(),
                event.contentId(),
                delta,
                properties.getProcessedEventTtl()
        );
    }

    private boolean isValid(ContentActivityKafkaEvent event) {
        return event != null
                && event.eventId() != null
                && event.eventType() != null
                && event.userId() != null
                && event.contentId() != null
                && event.occurredAt() != null;
    }
}
