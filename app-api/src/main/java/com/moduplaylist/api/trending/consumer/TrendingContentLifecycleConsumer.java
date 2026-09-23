package com.moduplaylist.api.trending.consumer;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentDeleted;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrendingContentLifecycleConsumer {

    private final TrendingContentRedisRepository trendingRedisRepository;

    @KafkaListener(
            topics = KafkaTopics.CONTENT_LIFECYCLE,
            groupId = "trending-content-lifecycle"
    )
    public void consume(ConsumerRecord<String, Object> record) {
        Object payload = record.value();
        if (payload instanceof ContentDeleted event && event.contentId() != null) {
            trendingRedisRepository.remove(event.contentId(), Instant.now());
        }
    }
}
