package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyCancelledKafkaEvent;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyUpdatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WatchPartyChangedKafkaListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUpdated(WatchPartyUpdatedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WATCH_PARTY_UPDATED,
                new WatchPartyUpdatedKafkaEvent(
                        event.eventId(),
                        event.partyId(),
                        event.title(),
                        event.previousScheduledAt(),
                        event.scheduledAt(),
                        event.recipientIds()
                )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCancelled(WatchPartyCancelledEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WATCH_PARTY_CANCELLED,
                new WatchPartyCancelledKafkaEvent(
                        event.eventId(),
                        event.partyId(),
                        event.title(),
                        event.scheduledAt(),
                        event.recipientIds()
                )
        );
    }
}