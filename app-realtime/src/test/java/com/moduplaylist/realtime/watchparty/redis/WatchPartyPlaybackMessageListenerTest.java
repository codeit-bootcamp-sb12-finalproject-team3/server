package com.moduplaylist.realtime.watchparty.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackState;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackStatus;
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

class WatchPartyPlaybackMessageListenerTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final WatchPartyPlaybackMessageListener listener =
            new WatchPartyPlaybackMessageListener(messagingTemplate, new ObjectMapper());

    // app-api가 발행하는 형태 그대로 (infrastructure WatchPartyPlaybackMessageContractTest와 짝 - 받는 쪽)
    @Test
    void app_api가_발행한_LIVE_상태를_받아_playback_구독_경로로_전달한다() {
        UUID partyId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        String channel = "watchparty:" + partyId + ":playback";
        String body = "{\"status\":\"LIVE\",\"startedAt\":1000,\"accumulatedPauseMs\":200,"
                + "\"pausedAt\":1500,\"startEpisode\":1,\"endEpisode\":3,"
                + "\"hostId\":\"" + hostId + "\",\"updatedAt\":2000}";

        listener.onMessage(new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8)), null);

        ArgumentCaptor<WatchPartyPlaybackState> captor =
                ArgumentCaptor.forClass(WatchPartyPlaybackState.class);
        verify(messagingTemplate).convertAndSend(
                eq("/sub/watch-parties/" + partyId + "/playback"), captor.capture());

        WatchPartyPlaybackState state = captor.getValue();
        assertThat(state.getStatus()).isEqualTo(WatchPartyPlaybackStatus.LIVE);
        assertThat(state.getStartedAt()).isEqualTo(1000L);
        assertThat(state.getAccumulatedPauseMs()).isEqualTo(200L);
        assertThat(state.getPausedAt()).isEqualTo(1500L);
        assertThat(state.getStartEpisode()).isEqualTo(1);
        assertThat(state.getEndEpisode()).isEqualTo(3);
        assertThat(state.getHostId()).isEqualTo(hostId);
        assertThat(state.getUpdatedAt()).isEqualTo(2000L);
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