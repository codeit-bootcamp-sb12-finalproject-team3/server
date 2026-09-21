package com.moduplaylist.realtime.watchparty.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.WatchPartyChatLogRegistry;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyChatMessage;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyChatSendRequest;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WatchPartyChatController {

    static final String CHANNEL_PREFIX = "watchparty:";
    static final String CHANNEL_SUFFIX = ":chat";

    private final WatchPartyChatLogRegistry watchPartyChatLogRegistry;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public WatchPartyChatController(
            WatchPartyChatLogRegistry watchPartyChatLogRegistry,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.watchPartyChatLogRegistry = watchPartyChatLogRegistry;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @MessageMapping("/watch-parties/{partyId}/chat")
    public void sendChat(
            @DestinationVariable UUID partyId,
            WatchPartyChatSendRequest request,
            Principal principal
    ) {
        UUID senderId = UUID.fromString(principal.getName());
        WatchPartyChatMessage message =
                new WatchPartyChatMessage(senderId, request.getContent(), Instant.now().toEpochMilli());

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
}