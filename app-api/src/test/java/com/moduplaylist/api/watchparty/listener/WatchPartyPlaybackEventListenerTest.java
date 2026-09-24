package com.moduplaylist.api.watchparty.listener;

import com.moduplaylist.api.watchparty.event.WatchPartyEndedEvent;
import com.moduplaylist.api.watchparty.event.WatchPartyStartedEvent;
import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
import com.moduplaylist.core.watchparty.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyPlaybackEventListenerTest {

    @Mock private WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    @Mock private WatchPartyKeyLifecycleRegistry watchPartyKeyLifecycleRegistry;
    @Mock private WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    @Mock private WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    @Mock private WatchPartyPlaybackBroadcaster watchPartyPlaybackBroadcaster;

    @InjectMocks
    private WatchPartyPlaybackEventListener watchPartyPlaybackEventListener;

    private UUID partyId;
    private WatchPartyPlaybackState endedState;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        endedState = new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.ENDED, 1000L, 0L, null,
                null, null, UUID.randomUUID(), 2000L
        );
    }

    // ===== 1. onWatchPartyStarted() =====

    // 케이스 1-1: 정상 시작 — Redis playback Hash 생성
    @Test
    void onWatchPartyStarted_정상_등록() {
        WatchPartyPlaybackState liveState = new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.LIVE, 1000L, 0L, null,
                null, null, UUID.randomUUID(), 1000L
        );
        WatchPartyStartedEvent event =
                new WatchPartyStartedEvent(UUID.randomUUID(), partyId, liveState);

        watchPartyPlaybackEventListener.onWatchPartyStarted(event);

        verify(watchPartyPlaybackRegistry).createOnLive(partyId, liveState);
    }

    // ===== 2. onWatchPartyEnded() =====

    // 케이스 2-1: 정상 종료 — markEnded → find → broadcast → TTL → joinedParty 정리까지 전부 수행
    @Test
    void onWatchPartyEnded_정상_처리() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        given(watchPartyPlaybackRegistry.find(partyId)).willReturn(Optional.of(endedState));
        given(watchPartyJoinedRegistry.findAll(partyId)).willReturn(Set.of(userId1, userId2));

        watchPartyPlaybackEventListener.onWatchPartyEnded(new WatchPartyEndedEvent(UUID.randomUUID(), partyId));

        verify(watchPartyPlaybackRegistry).markEnded(partyId);
        verify(watchPartyPlaybackBroadcaster).broadcastEnded(partyId, endedState);
        verify(watchPartyKeyLifecycleRegistry).armSafetyNetTtl(partyId);
        verify(watchPartyActivePartyRegistry).clearJoinedParty(userId1);
        verify(watchPartyActivePartyRegistry).clearJoinedParty(userId2);
    }

    // 케이스 2-2: markEnded가 실패해도 나머지 단계(브로드캐스트/TTL/정리)는 계속 진행됨
    @Test
    void onWatchPartyEnded_markEnded_실패해도_나머지_단계_계속_진행() {
        doThrow(new RuntimeException("Redis 장애")).when(watchPartyPlaybackRegistry).markEnded(partyId);
        given(watchPartyPlaybackRegistry.find(partyId)).willReturn(Optional.of(endedState));
        given(watchPartyJoinedRegistry.findAll(partyId)).willReturn(Set.of());

        watchPartyPlaybackEventListener.onWatchPartyEnded(new WatchPartyEndedEvent(UUID.randomUUID(), partyId));

        verify(watchPartyPlaybackBroadcaster).broadcastEnded(partyId, endedState);
        verify(watchPartyKeyLifecycleRegistry).armSafetyNetTtl(partyId);
    }

    // 케이스 2-3: find 결과가 없으면 브로드캐스트는 호출되지 않음 (나머지 단계는 정상 진행)
    @Test
    void onWatchPartyEnded_find_비어있으면_브로드캐스트_생략() {
        given(watchPartyPlaybackRegistry.find(partyId)).willReturn(Optional.empty());
        given(watchPartyJoinedRegistry.findAll(partyId)).willReturn(Set.of());

        watchPartyPlaybackEventListener.onWatchPartyEnded(new WatchPartyEndedEvent(UUID.randomUUID(), partyId));

        verify(watchPartyPlaybackBroadcaster, never()).broadcastEnded(any(), any());
        verify(watchPartyKeyLifecycleRegistry).armSafetyNetTtl(partyId);
    }
}