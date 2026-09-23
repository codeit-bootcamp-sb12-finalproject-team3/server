package com.moduplaylist.realtime.watchparty.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.WatchPartyChatLogRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyPlaybackRegistry;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyChatMessage;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyChatSendRequest;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WatchPartyChatController {

    static final String CHANNEL_PREFIX = "watchparty:";
    static final String CHANNEL_SUFFIX = ":chat";
    static final int CONTENT_MAX_LENGTH = 500; // 임시로 500자 정했습니다. 변경 가능.

    private final WatchPartyChatLogRegistry watchPartyChatLogRegistry;
    private final WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public WatchPartyChatController(
            WatchPartyChatLogRegistry watchPartyChatLogRegistry,
            WatchPartyPlaybackRegistry watchPartyPlaybackRegistry,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.watchPartyChatLogRegistry = watchPartyChatLogRegistry;
        this.watchPartyPlaybackRegistry = watchPartyPlaybackRegistry;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/watch-parties/{partyId}/chat")
    public void sendChat(
            @DestinationVariable UUID partyId,
            WatchPartyChatSendRequest request,
            Principal principal
    ) {
        UUID senderId = UUID.fromString(principal.getName());

        boolean ended = watchPartyPlaybackRegistry.find(partyId)
                .map(state -> state.getStatus() == WatchPartyPlaybackStatus.ENDED)
                .orElse(false);
        if (ended) {
            sendError(senderId, "이미 종료된 Watch Party입니다.");
            return;
        }

        String content = request.getContent();

        if (content == null || content.isBlank()) {
            sendError(senderId, "메시지 내용을 입력해주세요.");
            return;
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            sendError(senderId, "메시지는 최대 " + CONTENT_MAX_LENGTH + "자까지 입력할 수 있습니다.");
            return;
        }

        WatchPartyChatMessage message =
                new WatchPartyChatMessage(senderId, content, Instant.now().toEpochMilli());

        watchPartyChatLogRegistry.append(partyId, message);
        publish(partyId, message);
    }

    private void publish(UUID partyId, WatchPartyChatMessage message) {
        String channel = CHANNEL_PREFIX + partyId + CHANNEL_SUFFIX;
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("채팅 메시지 직렬화에 실패했습니다.", e);
        }
    }

    private void sendError(UUID userId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/errors", errorMessage);
    }
}