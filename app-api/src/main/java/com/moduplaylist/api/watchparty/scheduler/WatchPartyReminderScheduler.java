package com.moduplaylist.api.watchparty.scheduler;

import com.moduplaylist.core.watchparty.entity.WatchPartyReminder;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyReminderRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyReminderDueKafkaEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.moduplaylist.infrastructure.redis.lock.DistributedLockRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchPartyReminderScheduler {

    // Redis에 저장되는 실제 락 키
    private static final String LOCK_KEY = "scheduler:watchparty-reminder:lock";

    // 락 유효시간(TTL). 아래 @Scheduled의 fixedRate보다 반드시 짧아야 함
    // — 락을 쥔 인스턴스가 죽어도 다음 틱 전에 자동 해제되도록 하기 위함.
    // 정상 처리 시간(수 초)보다 충분히 여유 있게 잡은 값.
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    // 시작 몇 분 전에 리마인더를 발행할지 — 순수 정책값, 자유롭게 조정 가능
    // (다른 상수와 관계 없음)
    private static final Duration REMINDER_LEAD_TIME = Duration.ofMinutes(10);

    private final WatchPartyReminderRepository watchPartyReminderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DistributedLockRegistry distributedLockRegistry;

    // 1분마다 리마인더 스캔.
    // 값을 줄이면 알림 타이밍 정확도는 올라가지만 DB 조회 빈도도 늘어남.
    // 이 값을 바꾸면 위 LOCK_TTL이 여전히 이 값보다 작은지 같이 확인할 것.
    @Scheduled(fixedRate = 60000)
    public void runReminderTick() {
        String token = distributedLockRegistry.tryLock(LOCK_KEY, LOCK_TTL);
        if (token == null) {
            return; // 다른 인스턴스가 이미 이번 틱을 처리 중
        }
        try {
            processDueReminders();
        } finally {
            distributedLockRegistry.unlock(LOCK_KEY, token);
        }
    }

    private void processDueReminders() {
        Instant threshold = Instant.now().plus(REMINDER_LEAD_TIME);
        List<WatchPartyReminder> dueReminders =
                watchPartyReminderRepository.findDueReminders(WatchPartyStatus.SCHEDULED, threshold);

        for (WatchPartyReminder reminder : dueReminders) {
            UUID reminderId = reminder.getId();
            UUID partyId = reminder.getWatchParty().getId();
            UUID userId = reminder.getUser().getId();
            Instant scheduledAt = reminder.getWatchParty().getScheduledAt();

            try {
                // 삭제 먼저 — deleteById는 그 자체로 즉시 커밋되는 트랜잭션이라
                // 이 라인이 끝나면 DB에는 이미 반영된 상태
                watchPartyReminderRepository.deleteById(reminderId);

                // 그 다음 발행
                WatchPartyReminderDueKafkaEvent event = new WatchPartyReminderDueKafkaEvent(
                        UUID.randomUUID(), partyId, userId, scheduledAt);
                kafkaTemplate.send(KafkaTopics.WATCH_PARTY_REMINDER_DUE, event)
                        .whenComplete((result, exception) -> {
                            if (exception != null) {
                                log.warn(
                                        "watchparty reminder Kafka 발행 실패 - DB에서는 이미 삭제되어 재시도 불가. reminderId={}, partyId={}, userId={}",
                                        reminderId, partyId, userId, exception
                                );
                            }
                        });

                log.info("watchparty reminder sent - partyId={}, userId={}, scheduledAt={}",
                        partyId, userId, scheduledAt);
            } catch (Exception e) {
                log.error("watchparty reminder 처리 실패 - reminderId={}", reminderId, e);
            }
        }
    }

}