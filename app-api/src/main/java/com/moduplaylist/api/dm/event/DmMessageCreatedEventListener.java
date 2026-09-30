package com.moduplaylist.api.dm.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.DmMessageCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DmMessageCreatedEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DmMessageCreatedEvent event) {
        DmMessageCreatedKafkaEvent kafkaEvent = new DmMessageCreatedKafkaEvent(
                event.messageId(),
                event.conversationId(),
                event.senderId(),
                event.receiverId(),
                event.content(),
                event.createdAt()
        );

        kafkaTemplate.send(
                KafkaTopics.DM_MESSAGE_CREATED,
                event.conversationId().toString(),
                kafkaEvent
        );
    }
}
