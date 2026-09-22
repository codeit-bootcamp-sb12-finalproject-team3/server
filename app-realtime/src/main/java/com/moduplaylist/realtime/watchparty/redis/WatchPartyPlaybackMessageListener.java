package com.moduplaylist.realtime.watchparty.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WatchPartyPlaybackMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(WatchPartyPlaybackMessageListener.class);
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^watchparty:([^:]+):playback$");

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WatchPartyPlaybackMessageListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        Matcher matcher = CHANNEL_PATTERN.matcher(channel);
        if (!matcher.matches()) {
            return;
        }
        String partyId = matcher.group(1);

        try {
            WatchPartyPlaybackState state =
                    objectMapper.readValue(message.getBody(), WatchPartyPlaybackState.class);
            messagingTemplate.convertAndSend("/sub/watch-parties/" + partyId + "/playback", state);
        } catch (IOException e) {
            log.warn("Playback 상태 역직렬화 실패. channel={}", channel, e);
        }
    }
}