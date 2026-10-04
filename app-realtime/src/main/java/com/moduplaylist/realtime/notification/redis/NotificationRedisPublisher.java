package com.moduplaylist.realtime.notification.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationRedisPublisher {

    public static final String CHANNEL = "notification:created";

    private static final Logger log = LoggerFactory.getLogger(NotificationRedisPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public NotificationRedisPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationRedisMessage message) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            log.error("Notification Redis Pub/Sub publish failed. notificationId={}, receiverId={}",
                    message.notificationId(), message.receiverId(), exception);
        }
    }
}
