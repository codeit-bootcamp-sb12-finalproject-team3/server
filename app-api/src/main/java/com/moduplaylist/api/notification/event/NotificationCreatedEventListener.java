package com.moduplaylist.api.notification.event;

import com.moduplaylist.api.follow.event.FollowCreatedEvent;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.NotificationCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationCreatedEventListener {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationCreatedEvent event) {
        NotificationCreatedKafkaEvent kafkaEvent =
                new NotificationCreatedKafkaEvent(
                        event.notificationId(),
                        event.receiverId(),
                        event.title(),
                        event.content(),
                        event.level().name(),
                        event.createdAt()
                );

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_CREATED, kafkaEvent);
    }
}
