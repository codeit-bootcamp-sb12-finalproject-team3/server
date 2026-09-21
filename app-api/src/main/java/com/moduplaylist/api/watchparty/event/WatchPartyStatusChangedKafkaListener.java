package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyStatusChangedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WatchPartyStatusChangedKafkaListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStarted(WatchPartyStartedEvent event) {
        publish(event.partyId(), WatchPartyStatus.LIVE);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEnded(WatchPartyEndedEvent event) {
        publish(event.partyId(), WatchPartyStatus.ENDED);
    }

    private void publish(UUID watchPartyId, WatchPartyStatus status) {
        kafkaTemplate.send(
                KafkaTopics.WATCH_PARTY_STATUS_CHANGED,
                new WatchPartyStatusChangedKafkaEvent(watchPartyId, status)
        );
    }
}