package com.moduplaylist.realtime.watchparty.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyParticipantChangedMessage;
import com.moduplaylist.realtime.watchparty.websocket.WatchPartySubscriptionTerminator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WatchPartyParticipantMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(WatchPartyParticipantMessageListener.class);
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^watchparty:([^:]+):participants$");

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final WatchPartySubscriptionTerminator subscriptionTerminator;

    public WatchPartyParticipantMessageListener(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            WatchPartySubscriptionTerminator subscriptionTerminator
    ) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.subscriptionTerminator = subscriptionTerminator;
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
            WatchPartyParticipantChangedMessage changed =
                    objectMapper.readValue(message.getBody(), WatchPartyParticipantChangedMessage.class);
            messagingTemplate.convertAndSend("/sub/watch-parties/" + partyId + "/participants", changed);

            // KICKED는 방송 "후"에 기존 구독 해제 (본인도 KICKED를 먼저 받도록)
            if ("KICKED".equals(changed.getStatus()) && changed.getUserId() != null) {
                subscriptionTerminator.terminate(UUID.fromString(partyId), changed.getUserId());
            }
        } catch (IOException e) {
            log.warn("참가자 변경 메시지 역직렬화 실패. channel={}", channel, e);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 partyId 형식. channel={}", channel, e);
        }
    }
}