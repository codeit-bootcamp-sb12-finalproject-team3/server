package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyCancelledKafkaEvent;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyUpdatedKafkaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WatchPartyChangedNotificationConsumerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks
    private WatchPartyChangedNotificationConsumer consumer;

    // UTC 10:00 = 한국 시간 오후 7:00 / UTC 11:00 = 오후 8:00
    private static final Instant PREVIOUS = Instant.parse("2026-10-05T10:00:00Z");
    private static final Instant CHANGED = Instant.parse("2026-10-06T11:00:00Z");

    // 케이스 1: 시간 변경 — 받는 사람 전원에게, 이전→이후 날짜·시각(한국 시간) 포함 문구
    @Test
    void consumeUpdated_받는사람_전원에게_변경알림() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        WatchPartyUpdatedKafkaEvent event = new WatchPartyUpdatedKafkaEvent(
                UUID.randomUUID(), UUID.randomUUID(), "야식과 함께", PREVIOUS, CHANGED, List.of(userId1, userId2));

        consumer.consumeUpdated(event);

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        then(notificationService).should(times(2)).create(captor.capture());

        assertThat(captor.getAllValues()).extracting(NotificationCreateCommand::getReceiverId)
                .containsExactly(userId1, userId2);
        NotificationCreateCommand command = captor.getValue();
        assertThat(command.getTitle()).isEqualTo("Watch Party 시간 변경");
        assertThat(command.getContent())
                .isEqualTo("'야식과 함께' 같이보기 시작 시간이 바뀌었어요. 10월 5일 오후 7:00 → 10월 6일 오후 8:00");
        assertThat(command.getLevel()).isEqualTo(NotificationLevel.INFO);
    }

    // 케이스 2: 취소 — 원래 예정 시각을 포함한 취소 문구
    @Test
    void consumeCancelled_받는사람_전원에게_취소알림() {
        UUID userId = UUID.randomUUID();
        WatchPartyCancelledKafkaEvent event = new WatchPartyCancelledKafkaEvent(
                UUID.randomUUID(), UUID.randomUUID(), "야식과 함께", PREVIOUS, List.of(userId));

        consumer.consumeCancelled(event);

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        then(notificationService).should().create(captor.capture());

        NotificationCreateCommand command = captor.getValue();
        assertThat(command.getReceiverId()).isEqualTo(userId);
        assertThat(command.getTitle()).isEqualTo("Watch Party 취소");
        assertThat(command.getContent())
                .isEqualTo("10월 5일 오후 7:00 예정이던 '야식과 함께' 같이보기가 취소됐어요.");
    }

    // 케이스 3: 받는 사람이 없으면 알림 생성 없음
    @Test
    void consumeCancelled_받는사람_없으면_알림없음() {
        WatchPartyCancelledKafkaEvent event = new WatchPartyCancelledKafkaEvent(
                UUID.randomUUID(), UUID.randomUUID(), "야식과 함께", PREVIOUS, List.of());

        consumer.consumeCancelled(event);

        verifyNoInteractions(notificationService);
    }
}