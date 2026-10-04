package com.moduplaylist.realtime.notification.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.notification.sse.NotificationSsePayload;
import com.moduplaylist.realtime.notification.sse.NotificationSseService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationRedisMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationRedisMessageListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationSseService notificationSseService;

    public NotificationRedisMessageListener(
            ObjectMapper objectMapper,
            NotificationSseService notificationSseService
    ) {
        this.objectMapper = objectMapper;
        this.notificationSseService = notificationSseService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationRedisMessage notification =
                    objectMapper.readValue(message.getBody(), NotificationRedisMessage.class);
            NotificationSsePayload payload = new NotificationSsePayload(
                    notification.notificationId(),
                    notification.title(),
                    notification.content(),
                    notification.level(),
                    notification.createdAt()
            );
            notificationSseService.send(notification.receiverId(), payload);
        } catch (IOException exception) {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            log.warn("Notification Redis Pub/Sub message deserialization failed. channel={}", channel, exception);
        }
    }
}
