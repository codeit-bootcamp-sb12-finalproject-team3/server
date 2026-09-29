package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyReminderDueKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchPartyReminderNotificationConsumer {

    // 서버 시각은 UTC(Instant)로 다루고, 사용자에게 보여 줄 때만 한국 시간으로 변환
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter START_TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    private final NotificationService notificationService;
    private final WatchPartyRepository watchPartyRepository;

    @KafkaListener(
            topics = KafkaTopics.WATCH_PARTY_REMINDER_DUE,
            groupId = "notification-persistence"
    )
    public void consume(WatchPartyReminderDueKafkaEvent event) {
        WatchParty party = watchPartyRepository.findById(event.watchPartyId()).orElse(null);

        // 발행 후 파티가 삭제됐거나 이미 끝났으면 알릴 의미가 없음
        if (party == null || party.getStatus() == WatchPartyStatus.ENDED) {
            log.info("watchparty reminder 알림 생략 - partyId={}, userId={}",
                    event.watchPartyId(), event.userId());
            return;
        }

        String startTime = START_TIME_FORMAT.format(party.getScheduledAt().atZone(DISPLAY_ZONE));

        notificationService.create(new NotificationCreateCommand(
                event.userId(),
                "Watch Party 시작 임박",
                "알림을 설정한 '" + party.getTitle() + "' 같이보기가 " + startTime + "에 시작돼요.",
                NotificationLevel.INFO
        ));
    }
}