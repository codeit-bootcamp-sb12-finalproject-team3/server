package com.moduplaylist.api.watchparty.scheduler;

import com.moduplaylist.api.watchparty.service.WatchPartyGhostCleaner;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import com.moduplaylist.infrastructure.redis.lock.DistributedLockRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyGhostCleanupSchedulerTest {

    @Mock private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock private WatchPartyGhostCleaner watchPartyGhostCleaner;
    @Mock private DistributedLockRegistry distributedLockRegistry;

    @InjectMocks
    private WatchPartyGhostCleanupScheduler watchPartyGhostCleanupScheduler;

    private UUID partyA;
    private UUID partyB;

    @BeforeEach
    void setUp() {
        partyA = UUID.randomUUID();
        partyB = UUID.randomUUID();
    }

    // ===== runGhostCleanupTick() =====

    // 케이스 1: 락을 못 얻으면 — 다른 인스턴스가 처리 중이므로 아무것도 하지 않음
    @Test
    void runGhostCleanupTick_락_실패면_아무것도_안함() {
        given(distributedLockRegistry.tryLock(anyString(), any())).willReturn(null);

        watchPartyGhostCleanupScheduler.runGhostCleanupTick();

        verifyNoInteractions(watchPartyParticipantRepository, watchPartyGhostCleaner);
        verify(distributedLockRegistry, never()).unlock(anyString(), anyString());
    }

    // 케이스 2: 락을 얻으면 — 종료되지 않은 파티마다 정리하고 락 해제
    @Test
    void runGhostCleanupTick_파티마다_정리하고_락_해제() {
        given(distributedLockRegistry.tryLock(anyString(), any())).willReturn("token");
        given(watchPartyParticipantRepository.findPartyIdsHavingParticipantStatus(
                ParticipantStatus.JOINED, WatchPartyStatus.ENDED))
                .willReturn(List.of(partyA, partyB));

        watchPartyGhostCleanupScheduler.runGhostCleanupTick();

        verify(watchPartyGhostCleaner).cleanUpGhostsInParty(partyA);
        verify(watchPartyGhostCleaner).cleanUpGhostsInParty(partyB);
        verify(distributedLockRegistry).unlock(anyString(), eq("token"));
    }

    // 케이스 3: 한 파티에서 예외가 나도 — 다음 파티는 계속 정리하고 락도 해제
    @Test
    void runGhostCleanupTick_한_파티_실패해도_계속() {
        given(distributedLockRegistry.tryLock(anyString(), any())).willReturn("token");
        given(watchPartyParticipantRepository.findPartyIdsHavingParticipantStatus(
                ParticipantStatus.JOINED, WatchPartyStatus.ENDED))
                .willReturn(List.of(partyA, partyB));
        given(watchPartyGhostCleaner.cleanUpGhostsInParty(partyA))
                .willThrow(new IllegalStateException("db error"));

        watchPartyGhostCleanupScheduler.runGhostCleanupTick();

        verify(watchPartyGhostCleaner).cleanUpGhostsInParty(partyB);
        verify(distributedLockRegistry).unlock(anyString(), eq("token"));
    }
}