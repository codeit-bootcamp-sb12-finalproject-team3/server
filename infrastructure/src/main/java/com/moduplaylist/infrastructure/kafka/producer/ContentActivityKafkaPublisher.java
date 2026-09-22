package com.moduplaylist.infrastructure.kafka.producer;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentActivityKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(ContentActivityKafkaEvent event) {
        String key = event.userId() + ":" + event.contentId();

        try {
            kafkaTemplate.send(KafkaTopics.CONTENT_ACTIVITIES, key, event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn(
                                    "콘텐츠 활동 이벤트 발행에 실패했습니다. eventId={}, eventType={}, userId={}, contentId={}",
                                    event.eventId(),
                                    event.eventType(),
                                    event.userId(),
                                    event.contentId(),
                                    exception
                            );
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn(
                    "콘텐츠 활동 이벤트 발행 요청에 실패했습니다. eventId={}, eventType={}, userId={}, contentId={}",
                    event.eventId(),
                    event.eventType(),
                    event.userId(),
                    event.contentId(),
                    exception
            );
        }
    }
}
