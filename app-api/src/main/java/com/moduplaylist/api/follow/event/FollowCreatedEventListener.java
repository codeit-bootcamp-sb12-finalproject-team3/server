package com.moduplaylist.api.follow.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.FollowCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FollowCreatedEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowCreatedEvent event) {
        FollowCreatedKafkaEvent kafkaEvent =
                new FollowCreatedKafkaEvent(
                        event.followerId(),
                        event.followeeId()
                );

        kafkaTemplate.send(KafkaTopics.FOLLOW_CREATED, kafkaEvent);
    }
}