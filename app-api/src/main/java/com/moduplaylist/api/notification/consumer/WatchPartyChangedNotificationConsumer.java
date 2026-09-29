package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyCancelledKafkaEvent;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyUpdatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchPartyChangedNotificationConsumer {

    // 서버 시각은 UTC(Instant)로 다루고, 사용자에게 보여 줄 때만 한국 시간으로 변환
    // 날짜가 바뀌는 변경도 있으므로 #146 시작 임박 알림(a h:mm)과 달리 날짜까지 표시
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN);

    private final NotificationService notificationService;

    @KafkaListener(
            topics = KafkaTopics.WATCH_PARTY_UPDATED,
            groupId = "notification-persistence"
    )
    public void consumeUpdated(WatchPartyUpdatedKafkaEvent event) {
        String content = "'" + event.title() + "' 같이보기 시작 시간이 바뀌었어요. "
                + format(event.previousScheduledAt()) + " → " + format(event.scheduledAt());

        notifyRecipients(event.recipientIds(), "Watch Party 시간 변경", content);
        log.info("watchparty 시간 변경 알림 처리 - partyId={}, notified={}",
                event.watchPartyId(), event.recipientIds().size());
    }

    @KafkaListener(
            topics = KafkaTopics.WATCH_PARTY_CANCELLED,
            groupId = "notification-persistence"
    )
    public void consumeCancelled(WatchPartyCancelledKafkaEvent event) {
        String content = format(event.scheduledAt())
                + " 예정이던 '" + event.title() + "' 같이보기가 취소됐어요.";

        notifyRecipients(event.recipientIds(), "Watch Party 취소", content);
        log.info("watchparty 취소 알림 처리 - partyId={}, notified={}",
                event.watchPartyId(), event.recipientIds().size());
    }

    private void notifyRecipients(List<UUID> recipientIds, String title, String content) {
        for (UUID userId : recipientIds) {
            notificationService.create(new NotificationCreateCommand(
                    userId,
                    title,
                    content,
                    NotificationLevel.INFO
            ));
        }
    }

    private String format(Instant instant) {
        return DATE_TIME_FORMAT.format(instant.atZone(DISPLAY_ZONE));
    }
}