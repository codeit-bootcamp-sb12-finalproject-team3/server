package com.moduplaylist.api.watchparty.scheduler;

import com.moduplaylist.api.watchparty.service.WatchPartyStatusService;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackState;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.infrastructure.redis.lock.DistributedLockRegistry;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchPartyAutoEndScheduler {

    // Redis에 저장되는 실제 락 키
    private static final String LOCK_KEY = "scheduler:watchparty-auto-end:lock";

    // 락 유효시간(TTL). 아래 @Scheduled의 fixedRate(60초)보다 반드시 짧아야 함
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    // 예정 시간이 끝난 뒤, 방장이 직접 끄지 않았을 때 자동 종료까지 기다려 주는 여유 시간.
    // 아래 shouldAutoEnd()의 ①(재생 기준)과 ②(상한) 둘 다에 더해진다.
    static final Duration GRACE = Duration.ofMinutes(10);

    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    private final WatchPartyStatusService watchPartyStatusService;
    private final DistributedLockRegistry distributedLockRegistry;

    // 1분마다 진행 중(LIVE) 파티 중 종료 조건을 넘긴 파티를 자동 종료 (방장이 끄지 않고 나간 방 정리)
    // 이 값을 바꾸면 위 LOCK_TTL이 여전히 이 값보다 작은지 같이 확인할 것.
    @Scheduled(fixedRate = 60000)
    public void runAutoEndTick() {
        String token = distributedLockRegistry.tryLock(LOCK_KEY, LOCK_TTL);
        if (token == null) {
            return; // 다른 인스턴스가 이미 이번 틱을 처리 중
        }
        try {
            Instant now = Instant.now();
            for (WatchParty party : watchPartyRepository.findAllByStatus(WatchPartyStatus.LIVE)) {
                try {
                    WatchPartyPlaybackState state =
                            watchPartyPlaybackRegistry.find(party.getId()).orElse(null);
                    if (shouldAutoEnd(state, party.getSessionDurationMinutes(), party.getScheduledAt(), now)) {
                        watchPartyStatusService.autoEndIfLive(party.getId()); // 파티마다 별도 트랜잭션
                        log.info("Watch Party 자동 종료 - partyId={}", party.getId());
                    }
                } catch (Exception e) {
                    log.error("Watch Party 자동 종료 실패 - partyId={}", party.getId(), e);
                }
            }
        } finally {
            distributedLockRegistry.unlock(LOCK_KEY, token);
        }
    }

    /**
     * 자동 종료 여부 판단 (둘 중 하나라도 해당하면 종료). 순수 함수라 테스트하기 쉽게 분리했다.
     *
     * ① 재생 기준: 재생 경과 시간(일시정지 시간 제외) ≥ 예정 시간 + GRACE
     *    → 다 보고 방장이 끄지 않고 나간 방
     * ② 상한: 시작 예정 시각(scheduledAt)부터 흐른 시간 ≥ 예정 시간 × 2 + GRACE
     *    → 일시정지한 채 방치된 방 (일시정지 중에는 ①의 재생 시간이 늘지 않아 영원히 안 끝나므로)
     *    → startedAt이 아니라 scheduledAt 기준인 이유: SEEK(이동)하면 서버가 startedAt을 옮겨서
     *      벽시계 계산이 흔들린다. scheduledAt은 이동에 영향받지 않고, Redis 상태가 없어도 계산할 수 있다.
     *
     * 예) 예정 100분, GRACE 10분
     *    - 일시정지 없이 시청: 재생 110분째 종료(①)
     *    - 중간에 20분 휴식: 재생 110분째(벽시계 130분) 종료(①)
     *    - 50분째 일시정지 후 방치: 예정 시각 + 210분에 종료(②)
     */
    static boolean shouldAutoEnd(WatchPartyPlaybackState state, int sessionDurationMinutes,
                                 Instant scheduledAt, Instant now) {
        long plannedMs = Duration.ofMinutes(sessionDurationMinutes).toMillis();
        long graceMs = GRACE.toMillis();
        long nowMs = now.toEpochMilli();

        // ① 재생 경과 시간(일시정지 제외)이 예정 시간 + 여유를 넘음
        if (state != null && state.getStartedAt() != null) {
            long referenceMs = state.getPausedAt() != null ? state.getPausedAt() : nowMs;
            long pauseMs = state.getAccumulatedPauseMs() != null ? state.getAccumulatedPauseMs() : 0L;
            long playedMs = referenceMs - state.getStartedAt() - pauseMs;
            if (playedMs >= plannedMs + graceMs) {
                return true;
            }
        }

        // ② 일시정지 방치 대비 상한: 시작 예정 시각부터 예정 시간의 2배 + 여유가 지남
        long wallMs = nowMs - scheduledAt.toEpochMilli();
        return wallMs >= plannedMs * 2 + graceMs;
    }
}