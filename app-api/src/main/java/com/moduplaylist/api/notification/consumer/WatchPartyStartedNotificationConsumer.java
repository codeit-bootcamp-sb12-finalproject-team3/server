package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyReminderRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyStatusChangedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchPartyStartedNotificationConsumer {

    private final NotificationService notificationService;
    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyReminderRepository watchPartyReminderRepository;

    @KafkaListener(
            topics = KafkaTopics.WATCH_PARTY_STATUS_CHANGED,
            groupId = "notification-persistence"
    )
    public void consume(WatchPartyStatusChangedKafkaEvent event) {
        // 시작(LIVE)만 처리, 종료(ENDED)는 무시
        if (event.status() != WatchPartyStatus.LIVE) {
            return;
        }

        UUID partyId = event.watchPartyId();

        // 남은 리마인더 = 알림을 설정했지만 "곧 시작" 알림을 아직 못 받은 사람 (방장이 일찍 시작한 경우)
        List<UUID> userIds = watchPartyReminderRepository.findUserIdsByWatchPartyId(partyId);
        if (userIds.isEmpty()) {
            return;
        }

        // 파티가 삭제됐으면 리마인더도 cascade로 이미 지워짐
        WatchParty party = watchPartyRepository.findById(partyId).orElse(null);
        if (party == null) {
            return;
        }

        // 시작 직후 바로 종료됐으면 알림은 생략하고 리마인더만 정리
        if (party.getStatus() != WatchPartyStatus.ENDED) {
            String content = "알림을 설정한 '" + party.getTitle() + "' 같이보기가 시작됐어요.";
            for (UUID userId : userIds) {
                notificationService.create(new NotificationCreateCommand(
                        userId,
                        "Watch Party 시작",
                        content,
                        NotificationLevel.INFO
                ));
            }
        }

        // 알림 먼저 → 삭제: 중간에 실패해 재전달되면 중복은 생겨도 누락은 없음
        int deleted = watchPartyReminderRepository.deleteByWatchPartyId(partyId);
        log.info("watchparty 시작 알림 처리 - partyId={}, notified={}, deletedReminders={}",
                partyId, party.getStatus() != WatchPartyStatus.ENDED ? userIds.size() : 0, deleted);
    }
}