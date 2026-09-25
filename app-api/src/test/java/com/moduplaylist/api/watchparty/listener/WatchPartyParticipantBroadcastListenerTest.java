package com.moduplaylist.api.watchparty.listener;

import com.moduplaylist.api.watchparty.event.WatchPartyParticipantChangedEvent;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantBroadcaster;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantChangedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchPartyParticipantBroadcastListenerTest {

    @Mock private WatchPartyParticipantBroadcaster watchPartyParticipantBroadcaster;

    @InjectMocks
    private WatchPartyParticipantBroadcastListener listener;

    // 케이스 1: JOINED / LEFT / KICKED 모두 userId·status 그대로 방송
    @ParameterizedTest
    @EnumSource(ParticipantStatus.class)
    void 모든_상태를_그대로_방송한다(ParticipantStatus status) {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        listener.onParticipantChanged(
                new WatchPartyParticipantChangedEvent(UUID.randomUUID(), partyId, userId, status, false));

        ArgumentCaptor<WatchPartyParticipantChangedMessage> captor =
                ArgumentCaptor.forClass(WatchPartyParticipantChangedMessage.class);
        verify(watchPartyParticipantBroadcaster).broadcast(eq(partyId), captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getStatus()).isEqualTo(status);
    }

    // 케이스 2: 재참가(isRejoin=true)도 방송 (Kafka 리스너와 다른 규칙)
    @Test
    void 재참가도_방송한다() {
        UUID partyId = UUID.randomUUID();

        listener.onParticipantChanged(new WatchPartyParticipantChangedEvent(
                UUID.randomUUID(), partyId, UUID.randomUUID(), ParticipantStatus.JOINED, true));

        verify(watchPartyParticipantBroadcaster).broadcast(eq(partyId), any());
    }

    // 케이스 3: 방송이 실패해도 예외가 밖으로 새지 않음
    @Test
    void 방송_실패해도_예외를_던지지_않는다() {
        doThrow(new RuntimeException("redis down"))
                .when(watchPartyParticipantBroadcaster).broadcast(any(), any());

        assertThatCode(() -> listener.onParticipantChanged(new WatchPartyParticipantChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ParticipantStatus.LEFT, false)))
                .doesNotThrowAnyException();
    }
}