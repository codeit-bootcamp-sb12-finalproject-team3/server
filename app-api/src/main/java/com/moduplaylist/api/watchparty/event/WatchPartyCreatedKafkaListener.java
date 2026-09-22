package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WatchPartyCreatedKafkaListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WatchPartyCreatedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WATCH_PARTY_CREATED,
                new WatchPartyCreatedKafkaEvent(
                        event.eventId(),
                        event.partyId(),
                        event.hostId(),
                        event.contentId(),
                        event.scheduledAt()
                )
        );
    }
}