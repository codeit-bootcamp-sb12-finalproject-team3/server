package com.moduplaylist.realtime.dm.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.dm.websocket.DmMessageCreatedPayload;
import com.moduplaylist.realtime.dm.websocket.DmRealtimeDeliveryService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class DmRedisMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(DmRedisMessageListener.class);

    private final ObjectMapper objectMapper;
    private final DmRealtimeDeliveryService deliveryService;

    public DmRedisMessageListener(
            ObjectMapper objectMapper,
            DmRealtimeDeliveryService deliveryService
    ) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            DmRedisMessage dmMessage = objectMapper.readValue(message.getBody(), DmRedisMessage.class);
            DmMessageCreatedPayload payload = new DmMessageCreatedPayload(
                    dmMessage.messageId(),
                    dmMessage.conversationId(),
                    dmMessage.senderId(),
                    dmMessage.receiverId(),
                    dmMessage.content(),
                    dmMessage.createdAt()
            );
            deliveryService.deliver(dmMessage.senderId(), dmMessage.receiverId(), payload);
        } catch (IOException exception) {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            log.warn("DM Redis Pub/Sub message deserialization failed. channel={}", channel, exception);
        }
    }
}
