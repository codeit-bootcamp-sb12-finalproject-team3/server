package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyParticipantChangedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WatchPartyParticipantChangedKafkaListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WatchPartyParticipantChangedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WATCH_PARTY_PARTICIPANT_CHANGED,
                new WatchPartyParticipantChangedKafkaEvent(
                        event.partyId(), event.userId(), event.status()
                )
        );
    }
}