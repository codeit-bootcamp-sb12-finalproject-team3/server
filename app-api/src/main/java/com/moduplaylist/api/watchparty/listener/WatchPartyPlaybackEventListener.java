package com.moduplaylist.api.watchparty.listener;

import com.moduplaylist.api.watchparty.event.WatchPartyEndedEvent;
import com.moduplaylist.api.watchparty.event.WatchPartyStartedEvent;
import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
import com.moduplaylist.core.watchparty.repository.*;
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
    private final WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    private final WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    private final WatchPartyPlaybackBroadcaster watchPartyPlaybackBroadcaster;


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
            watchPartyPlaybackRegistry.markEnded(event.partyId());
        } catch (Exception e) {
            log.error("Watch Party 종료 - Redis playback 상태 갱신 실패. partyId={}", event.partyId(), e);
        }

        try {
            watchPartyPlaybackRegistry.find(event.partyId())
                    .map(this::withEndedStatus)
                    .ifPresent(state -> watchPartyPlaybackBroadcaster.broadcastEnded(event.partyId(), state));
        } catch (Exception e) {
            log.error("Watch Party 종료 - Redis Pub/Sub 브로드캐스트 실패. partyId={}", event.partyId(), e);
        }

        try {
            watchPartyKeyLifecycleRegistry.armSafetyNetTtl(event.partyId());
        } catch (Exception e) {
            log.error("Watch Party 종료 - Redis TTL 반영 실패. partyId={}", event.partyId(), e);
        }

        try {
            // joinedParty 역인덱스는 파티 키 그룹에 안 묶이니 여기서 별도 정리
            watchPartyJoinedRegistry.findAll(event.partyId())
                    .forEach(watchPartyActivePartyRegistry::clearJoinedParty);
        } catch (Exception e) {
            log.error("Watch Party 종료 - joinedParty 역인덱스 정리 실패. partyId={}", event.partyId(), e);
        }
    }

    private WatchPartyPlaybackState withEndedStatus(WatchPartyPlaybackState state) {
        return new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.ENDED,
                state.getStartedAt(),
                state.getAccumulatedPauseMs(),
                state.getPausedAt(),
                state.getStartEpisode(),
                state.getEndEpisode(),
                state.getHostId(),
                System.currentTimeMillis()
        );
    }
}