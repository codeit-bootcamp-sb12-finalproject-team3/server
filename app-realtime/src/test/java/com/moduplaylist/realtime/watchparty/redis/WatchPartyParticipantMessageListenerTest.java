package com.moduplaylist.realtime.watchparty.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyParticipantChangedMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WatchPartyParticipantMessageListenerTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final WatchPartyParticipantMessageListener listener =
            new WatchPartyParticipantMessageListener(messagingTemplate, new ObjectMapper());

    // app-api가 발행하는 형태 그대로 (infrastructure WatchPartyParticipantMessageContractTest와 짝)
    @Test
    void app_api가_발행한_JSON을_받아_참가자_구독_경로로_전달한다() {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String channel = "watchparty:" + partyId + ":participants";
        String body = "{\"userId\":\"" + userId + "\",\"status\":\"KICKED\"}";

        listener.onMessage(new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8)), null);

        ArgumentCaptor<WatchPartyParticipantChangedMessage> captor =
                ArgumentCaptor.forClass(WatchPartyParticipantChangedMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/sub/watch-parties/" + partyId + "/participants"), captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getStatus()).isEqualTo("KICKED");
    }

    @Test
    void 다른_채널_메시지는_무시한다() {
        String channel = "watchparty:" + UUID.randomUUID() + ":chat";

        listener.onMessage(new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "{}".getBytes(StandardCharsets.UTF_8)), null);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}