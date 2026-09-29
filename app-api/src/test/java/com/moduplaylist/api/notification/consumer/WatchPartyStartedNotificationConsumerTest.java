package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyReminderRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.infrastructure.kafka.event.WatchPartyStatusChangedKafkaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WatchPartyStartedNotificationConsumerTest {

    @Mock private NotificationService notificationService;
    @Mock private WatchPartyRepository watchPartyRepository;
    @Mock private WatchPartyReminderRepository watchPartyReminderRepository;

    @InjectMocks
    private WatchPartyStartedNotificationConsumer consumer;

    private UUID partyId;
    private UUID userId1;
    private UUID userId2;
    private WatchParty watchParty;
    private WatchPartyStatusChangedKafkaEvent liveEvent;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        watchParty = Mockito.mock(WatchParty.class);
        liveEvent = new WatchPartyStatusChangedKafkaEvent(UUID.randomUUID(), partyId, WatchPartyStatus.LIVE);
    }

    // 케이스 1: 남은 리마인더 설정자 전원에게 시작 알림 → 그 뒤 리마인더 삭제
    @Test
    void consume_LIVE_남은_리마인더_전원_알림_후_삭제() {
        given(watchPartyReminderRepository.findUserIdsByWatchPartyId(partyId))
                .willReturn(List.of(userId1, userId2));
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.LIVE);
        given(watchParty.getTitle()).willReturn("야식과 함께");

        consumer.consume(liveEvent);

        ArgumentCaptor<NotificationCreateCommand> captor =
                ArgumentCaptor.forClass(NotificationCreateCommand.class);
        then(notificationService).should(times(2)).create(captor.capture());

        List<NotificationCreateCommand> commands = captor.getAllValues();
        assertThat(commands).extracting(NotificationCreateCommand::getReceiverId)
                .containsExactly(userId1, userId2);

        NotificationCreateCommand command = commands.get(0);
        assertThat(command.getTitle()).isEqualTo("Watch Party 시작");
        assertThat(command.getContent())
                .isEqualTo("알림을 설정한 '야식과 함께' 같이보기가 시작됐어요.");
        assertThat(command.getLevel()).isEqualTo(NotificationLevel.INFO);

        // 알림 먼저 → 삭제 순서
        InOrder inOrder = inOrder(notificationService, watchPartyReminderRepository);
        inOrder.verify(notificationService, times(2)).create(any());
        inOrder.verify(watchPartyReminderRepository).deleteByWatchPartyId(partyId);
    }

    // 케이스 2: 종료(ENDED) 이벤트는 무시
    @Test
    void consume_ENDED_이벤트는_무시() {
        WatchPartyStatusChangedKafkaEvent endedEvent =
                new WatchPartyStatusChangedKafkaEvent(UUID.randomUUID(), partyId, WatchPartyStatus.ENDED);

        consumer.consume(endedEvent);

        verifyNoInteractions(watchPartyReminderRepository, watchPartyRepository, notificationService);
    }

    // 케이스 3: 남은 리마인더가 없으면 파티 조회·알림·삭제 모두 없음 (대부분의 시작)
    @Test
    void consume_남은_리마인더_없으면_아무것도_안함() {
        given(watchPartyReminderRepository.findUserIdsByWatchPartyId(partyId)).willReturn(List.of());

        consumer.consume(liveEvent);

        verifyNoInteractions(watchPartyRepository, notificationService);
        then(watchPartyReminderRepository).should(never()).deleteByWatchPartyId(any());
    }

    // 케이스 4: 파티가 삭제됐으면 알림·삭제 없음 (cascade로 이미 지워짐)
    @Test
    void consume_파티없으면_알림_삭제_생략() {
        given(watchPartyReminderRepository.findUserIdsByWatchPartyId(partyId))
                .willReturn(List.of(userId1));
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.empty());

        consumer.consume(liveEvent);

        verifyNoInteractions(notificationService);
        then(watchPartyReminderRepository).should(never()).deleteByWatchPartyId(any());
    }

    // 케이스 5: 시작 직후 이미 종료됐으면 알림은 생략, 리마인더는 정리
    @Test
    void consume_이미_종료된_파티면_알림없이_삭제만() {
        given(watchPartyReminderRepository.findUserIdsByWatchPartyId(partyId))
                .willReturn(List.of(userId1));
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.ENDED);

        consumer.consume(liveEvent);

        verifyNoInteractions(notificationService);
        then(watchPartyReminderRepository).should().deleteByWatchPartyId(partyId);
    }

    // 케이스 6: 알림 생성 중 실패하면 예외 전파 + 삭제 안 함 → 재전달 시 다시 보냄 (누락 방지)
    @Test
    void consume_알림_실패하면_삭제하지_않고_예외_전파() {
        given(watchPartyReminderRepository.findUserIdsByWatchPartyId(partyId))
                .willReturn(List.of(userId1));
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.LIVE);
        given(watchParty.getTitle()).willReturn("야식과 함께");
        willThrow(new RuntimeException("DB 오류")).given(notificationService).create(any());

        assertThatThrownBy(() -> consumer.consume(liveEvent))
                .isInstanceOf(RuntimeException.class);

        then(watchPartyReminderRepository).should(never()).deleteByWatchPartyId(any());
    }
}