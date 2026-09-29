package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyReminderDueKafkaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchPartyReminderNotificationConsumerTest {

    @Mock private NotificationService notificationService;
    @Mock private WatchPartyRepository watchPartyRepository;

    @InjectMocks
    private WatchPartyReminderNotificationConsumer consumer;

    // UTC 11:00 = 한국 시간 오후 8:00
    private static final Instant SCHEDULED_AT = Instant.parse("2026-09-28T11:00:00Z");

    private UUID partyId;
    private UUID userId;
    private WatchParty watchParty;
    private WatchPartyReminderDueKafkaEvent event;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        watchParty = Mockito.mock(WatchParty.class);
        event = new WatchPartyReminderDueKafkaEvent(UUID.randomUUID(), partyId, userId, SCHEDULED_AT);
    }

    // 케이스 1: 시작 대기 파티 — 한국 시각이 들어간 시작 임박 알림 생성
    @Test
    void consume_정상_시작임박_알림_생성() {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.SCHEDULED);
        given(watchParty.getScheduledAt()).willReturn(SCHEDULED_AT);
        given(watchParty.getTitle()).willReturn("야식과 함께");

        consumer.consume(event);

        ArgumentCaptor<NotificationCreateCommand> captor =
                ArgumentCaptor.forClass(NotificationCreateCommand.class);
        then(notificationService).should().create(captor.capture());

        NotificationCreateCommand command = captor.getValue();
        assertThat(command.getReceiverId()).isEqualTo(userId);
        assertThat(command.getTitle()).isEqualTo("Watch Party 시작 임박");
        assertThat(command.getContent())
                .isEqualTo("알림을 설정한 '야식과 함께' 같이보기가 오후 8:00에 시작돼요.");
        assertThat(command.getLevel()).isEqualTo(NotificationLevel.INFO);
    }

    // 케이스 2: 방장이 먼저 시작해 LIVE여도 알림은 생성 (지금 들어가면 됨)
    @Test
    void consume_이미_LIVE여도_알림_생성() {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.LIVE);
        given(watchParty.getScheduledAt()).willReturn(SCHEDULED_AT);
        given(watchParty.getTitle()).willReturn("야식과 함께");

        consumer.consume(event);

        then(notificationService).should().create(any(NotificationCreateCommand.class));
    }

    // 케이스 3: 발행 후 파티가 삭제됐으면 알림 생성 안 함
    @Test
    void consume_파티없으면_알림_생략() {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.empty());

        consumer.consume(event);

        then(notificationService).should(never()).create(any());
    }

    // 케이스 4: 이미 종료된 파티면 알림 생성 안 함
    @Test
    void consume_ENDED면_알림_생략() {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.ENDED);

        consumer.consume(event);

        then(notificationService).should(never()).create(any());
    }
}