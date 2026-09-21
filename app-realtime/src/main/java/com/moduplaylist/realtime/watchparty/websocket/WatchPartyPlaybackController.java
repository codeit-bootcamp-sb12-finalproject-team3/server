package com.moduplaylist.realtime.watchparty.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.WatchPartyHostRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyPlaybackRegistry;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackAction;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackControlRequest;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackState;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackStatus;
import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WatchPartyPlaybackController {

    static final String CHANNEL_PREFIX = "watchparty:";
    static final String CHANNEL_SUFFIX = ":playback";

    private final WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    private final WatchPartyHostRegistry watchPartyHostRegistry;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public WatchPartyPlaybackController(
            WatchPartyPlaybackRegistry watchPartyPlaybackRegistry,
            WatchPartyHostRegistry watchPartyHostRegistry,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.watchPartyPlaybackRegistry = watchPartyPlaybackRegistry;
        this.watchPartyHostRegistry = watchPartyHostRegistry;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/watch-parties/{partyId}/playback")
    public void control(
            @DestinationVariable UUID partyId,
            WatchPartyPlaybackControlRequest request,
            Principal principal
    ) {
        UUID userId = UUID.fromString(principal.getName());

        if (!watchPartyHostRegistry.isHost(partyId, userId)) {
            sendError(userId, "방장만 재생을 제어할 수 있습니다.");
            return;
        }

        Optional<WatchPartyPlaybackState> current = watchPartyPlaybackRegistry.find(partyId);
        if (current.isEmpty()) {
            sendError(userId, "아직 시작되지 않은 Watch Party입니다.");
            return;
        }

        WatchPartyPlaybackState state = current.get();

        String invalidReason = invalidTransitionReason(state.getStatus(), request);
        if (invalidReason != null) {
            sendError(userId, invalidReason);
            return;
        }

        WatchPartyPlaybackState next = nextState(state, request);
        watchPartyPlaybackRegistry.update(partyId, next);
        publish(partyId, next);
    }

    private WatchPartyPlaybackState nextState(WatchPartyPlaybackState s, WatchPartyPlaybackControlRequest request) {
        long now = Instant.now().toEpochMilli();
        return switch (request.getAction()) {
            case PAUSE -> pause(s, now);
            case PLAY -> resume(s, now);
            case SEEK -> seek(s, now, request.getTargetElapsedMs());
        };
    }

    private String invalidTransitionReason(WatchPartyPlaybackStatus status, WatchPartyPlaybackControlRequest request) {
        if (status == WatchPartyPlaybackStatus.ENDED) {
            return "이미 종료된 Watch Party입니다.";
        }
        if (request.getAction() == WatchPartyPlaybackAction.PLAY && status == WatchPartyPlaybackStatus.LIVE) {
            return "이미 재생 중입니다.";
        }
        if (request.getAction() == WatchPartyPlaybackAction.PAUSE && status == WatchPartyPlaybackStatus.PAUSED) {
            return "이미 일시정지 상태입니다.";
        }
        if (request.getAction() == WatchPartyPlaybackAction.SEEK && request.getTargetElapsedMs() == null) {
            return "SEEK 요청에는 targetElapsedMs가 필요합니다.";
        }
        return null;
    }

    private WatchPartyPlaybackState pause(WatchPartyPlaybackState s, long now) {
        return new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.PAUSED,
                s.getStartedAt(), s.getAccumulatedPauseMs(),
                now,
                s.getStartEpisode(), s.getEndEpisode(), s.getHostId(), now
        );
    }

    private WatchPartyPlaybackState resume(WatchPartyPlaybackState s, long now) {
        long pauseDuration = now - s.getPausedAt();
        return new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.LIVE,
                s.getStartedAt(), s.getAccumulatedPauseMs() + pauseDuration,
                null,
                s.getStartEpisode(), s.getEndEpisode(), s.getHostId(), now
        );
    }

    private WatchPartyPlaybackState seek(WatchPartyPlaybackState s, long now, long targetElapsedMs) {
        boolean paused = s.getStatus() == WatchPartyPlaybackStatus.PAUSED;
        long referenceTime = paused ? s.getPausedAt() : now;
        long newStartedAt = referenceTime - s.getAccumulatedPauseMs() - targetElapsedMs;

        return new WatchPartyPlaybackState(
                s.getStatus(), newStartedAt, s.getAccumulatedPauseMs(),
                s.getPausedAt(),
                s.getStartEpisode(), s.getEndEpisode(), s.getHostId(), now
        );
    }

    private void publish(UUID partyId, WatchPartyPlaybackState state) {
        String channel = CHANNEL_PREFIX + partyId + CHANNEL_SUFFIX;
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("재생 상태 직렬화에 실패했습니다.", e);
        }
    }

    private void sendError(UUID userId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/errors", errorMessage);
    }
}