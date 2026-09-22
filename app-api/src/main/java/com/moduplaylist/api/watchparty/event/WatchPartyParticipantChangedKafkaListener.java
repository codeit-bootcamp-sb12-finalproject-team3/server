package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyParticipantChangedKafkaEvent;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyParticipantJoinedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WatchPartyParticipantChangedKafkaListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WatchPartyRepository watchPartyRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WatchPartyParticipantChangedEvent event) {
        if (event.status() == ParticipantStatus.JOINED) {
            if (event.isRejoin()) {
                return; // 재참가는 발행하지 않음 (트렌딩/추천 컨슈머 합의)
            }
            publishJoined(event);
            return;
        }
        publishStatusChanged(event);
    }

    private void publishJoined(WatchPartyParticipantChangedEvent event) {
        UUID contentId = watchPartyRepository.findById(event.partyId())
                .map(WatchParty::getContentId)
                .orElse(null);

        kafkaTemplate.send(
                KafkaTopics.WATCH_PARTY_PARTICIPANT_CHANGED,
                new WatchPartyParticipantJoinedKafkaEvent(
                        event.eventId(), event.partyId(), event.userId(), contentId, Instant.now()
                )
        );
    }

    private void publishStatusChanged(WatchPartyParticipantChangedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WATCH_PARTY_PARTICIPANT_STATUS_CHANGED,
                new WatchPartyParticipantChangedKafkaEvent(
                        event.eventId(), event.partyId(), event.userId(), event.status(), event.isRejoin()
                )
        );
    }
}