package com.moduplaylist.api.watchparty.scheduler;

import com.moduplaylist.api.watchparty.service.WatchPartyStatusService;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.infrastructure.redis.lock.DistributedLockRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchPartyAutoStartScheduler {

    // Redis에 저장되는 실제 락 키
    private static final String LOCK_KEY = "scheduler:watchparty-auto-start:lock";

    // 락 유효시간(TTL). 아래 @Scheduled의 fixedRate(10초)보다 반드시 짧아야 함
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    // 시작 시각이 이보다 오래 지난 파티는 자동 시작하지 않음 (배포 직후 옛 파티 일괄 시작·알림 폭주 방지)
    private static final Duration CATCH_UP_WINDOW = Duration.ofHours(1);

    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyStatusService watchPartyStatusService;
    private final DistributedLockRegistry distributedLockRegistry;

    // 10초마다 시작 시각이 된 SCHEDULED 파티를 LIVE로 전환. 최대 지연 약 10초
    // 이 값을 바꾸면 위 LOCK_TTL이 여전히 이 값보다 작은지 같이 확인할 것.
    @Scheduled(fixedRate = 10000)
    public void runAutoStartTick() {
        String token = distributedLockRegistry.tryLock(LOCK_KEY, LOCK_TTL);
        if (token == null) {
            return; // 다른 인스턴스가 이미 이번 틱을 처리 중
        }
        try {
            Instant now = Instant.now();
            List<UUID> partyIds = watchPartyRepository.findIdsToAutoStart(
                    WatchPartyStatus.SCHEDULED, now, now.minus(CATCH_UP_WINDOW));

            for (UUID partyId : partyIds) {
                try {
                    watchPartyStatusService.autoStartIfScheduled(partyId); // 파티마다 별도 트랜잭션
                    log.info("Watch Party 자동 시작 - partyId={}", partyId);
                } catch (Exception e) {
                    log.error("Watch Party 자동 시작 실패 - partyId={}", partyId, e);
                }
            }
        } finally {
            distributedLockRegistry.unlock(LOCK_KEY, token);
        }
    }
}