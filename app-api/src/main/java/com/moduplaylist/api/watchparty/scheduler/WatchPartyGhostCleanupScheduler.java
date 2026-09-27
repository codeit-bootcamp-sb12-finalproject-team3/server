package com.moduplaylist.api.watchparty.scheduler;

import com.moduplaylist.api.watchparty.service.WatchPartyGhostCleaner;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import com.moduplaylist.infrastructure.redis.lock.DistributedLockRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchPartyGhostCleanupScheduler {

    // Redis에 저장되는 실제 락 키
    private static final String LOCK_KEY = "scheduler:watchparty-ghost-cleanup:lock";

    // 락 유효시간(TTL). 아래 @Scheduled의 fixedRate보다 반드시 짧아야 함
    // — 락을 쥔 인스턴스가 죽어도 다음 틱 전에 자동 해제되도록 하기 위함.
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final WatchPartyGhostCleaner watchPartyGhostCleaner;
    private final DistributedLockRegistry distributedLockRegistry;

    // 1분마다 종료되지 않은 파티의 유령 JOINED 정리.
    // 이 값을 바꾸면 위 LOCK_TTL이 여전히 이 값보다 작은지 같이 확인할 것.
    // initialDelay: 서버가 뜬 직후엔 realtime 하트비트가 아직 lastSeen을 채우기 전일 수 있다.
    // (첫 배포, realtime 재시작 등) 유령 기준(5분) + 하트비트 주기(1분)만큼 기다려 거짓 오프라인을 막는다.
    @Scheduled(fixedRate = 60000, initialDelay = 360000)
    public void runGhostCleanupTick() {
        String token = distributedLockRegistry.tryLock(LOCK_KEY, LOCK_TTL);
        if (token == null) {
            return; // 다른 인스턴스가 이미 이번 틱을 처리 중
        }
        try {
            cleanUpAllParties();
        } finally {
            distributedLockRegistry.unlock(LOCK_KEY, token);
        }
    }

    private void cleanUpAllParties() {
        List<UUID> partyIds = watchPartyParticipantRepository
                .findPartyIdsHavingParticipantStatus(ParticipantStatus.JOINED, WatchPartyStatus.ENDED);

        int totalCleaned = 0;
        for (UUID partyId : partyIds) {
            try {
                totalCleaned += watchPartyGhostCleaner.cleanUpGhostsInParty(partyId);
            } catch (Exception e) {
                // 한 파티 실패가 나머지 파티 정리를 막지 않도록
                log.error("유령 JOINED 정리 실패 - partyId={}", partyId, e);
            }
        }

        if (totalCleaned > 0) {
            log.info("유령 JOINED 정리 틱 완료 - 확인 파티 {}개, 정리 {}명", partyIds.size(), totalCleaned);
        }
    }
}