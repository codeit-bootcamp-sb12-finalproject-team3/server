package com.moduplaylist.api.watchparty.listener;

import com.moduplaylist.api.watchparty.event.WatchPartyParticipantChangedEvent;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantBroadcaster;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantChangedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchPartyParticipantBroadcastListener {

    private final WatchPartyParticipantBroadcaster watchPartyParticipantBroadcaster;

    // JOINED(신규·재참가) / LEFT / KICKED 모두 방송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantChanged(WatchPartyParticipantChangedEvent event) {
        try {
            watchPartyParticipantBroadcaster.broadcast(
                    event.partyId(),
                    new WatchPartyParticipantChangedMessage(event.userId(), event.status())
            );
        } catch (Exception e) {
            log.error("Watch Party 참가자 변경 브로드캐스트 실패. partyId={}, userId={}",
                    event.partyId(), event.userId(), e);
        }
    }
}