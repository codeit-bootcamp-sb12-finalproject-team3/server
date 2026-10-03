package com.moduplaylist.realtime.notification.consumer;

import com.moduplaylist.realtime.kafka.KafkaTopics;
import com.moduplaylist.realtime.kafka.event.NotificationCreatedKafkaEvent;
import com.moduplaylist.realtime.notification.redis.NotificationRedisMessage;
import com.moduplaylist.realtime.notification.redis.NotificationRedisPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationCreatedConsumer {

    private final NotificationRedisPublisher notificationRedisPublisher;

    public NotificationCreatedConsumer(NotificationRedisPublisher notificationRedisPublisher) {
        this.notificationRedisPublisher = notificationRedisPublisher;
    }

    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_CREATED,
            groupId = "${realtime.kafka.notification-consumer-group}"
    )
    public void consume(NotificationCreatedKafkaEvent event) {
        NotificationRedisMessage message = new NotificationRedisMessage(
                event.receiverId(),
                event.notificationId(),
                event.title(),
                event.content(),
                event.level(),
                event.createdAt()
        );

        notificationRedisPublisher.publish(message);
    }
}
