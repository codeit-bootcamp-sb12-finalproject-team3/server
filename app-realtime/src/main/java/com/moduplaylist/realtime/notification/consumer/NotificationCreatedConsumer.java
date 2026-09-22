package com.moduplaylist.realtime.notification.consumer;

import com.moduplaylist.realtime.kafka.KafkaTopics;
import com.moduplaylist.realtime.kafka.event.NotificationCreatedKafkaEvent;
import com.moduplaylist.realtime.notification.sse.NotificationSsePayload;
import com.moduplaylist.realtime.notification.sse.NotificationSseService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationCreatedConsumer {

    private final NotificationSseService notificationSseService;

    public NotificationCreatedConsumer(NotificationSseService notificationSseService) {
        this.notificationSseService = notificationSseService;
    }

    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_CREATED,
            groupId = "${realtime.kafka.notification-consumer-group}"
    )
    public void consume(NotificationCreatedKafkaEvent event) {
        NotificationSsePayload payload = new NotificationSsePayload(
                event.notificationId(),
                event.title(),
                event.content(),
                event.level(),
                event.createdAt()
        );

        notificationSseService.send(event.receiverId(), payload);
    }
}
