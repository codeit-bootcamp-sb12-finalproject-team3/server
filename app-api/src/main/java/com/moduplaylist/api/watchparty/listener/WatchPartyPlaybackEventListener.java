package com.moduplaylist.api.watchparty.listener;

import com.moduplaylist.api.watchparty.event.WatchPartyEndedEvent;
import com.moduplaylist.api.watchparty.event.WatchPartyStartedEvent;
import com.moduplaylist.core.watchparty.repository.WatchPartyKeyLifecycleRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchPartyPlaybackEventListener {

    private final WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    private final WatchPartyKeyLifecycleRegistry watchPartyKeyLifecycleRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWatchPartyStarted(WatchPartyStartedEvent event) {
        try {
            watchPartyPlaybackRegistry.createOnLive(event.partyId(), event.playbackState());
        } catch (Exception e) {
            log.error("Watch Party 시작 - Redis playback 반영 실패. partyId={}", event.partyId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWatchPartyEnded(WatchPartyEndedEvent event) {
        try {
            watchPartyKeyLifecycleRegistry.armSafetyNetTtl(event.partyId());
        } catch (Exception e) {
            log.error("Watch Party 종료 - Redis TTL 반영 실패. partyId={}", event.partyId(), e);
        }
    }
}