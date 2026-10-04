package com.moduplaylist.realtime.dm.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DmRedisPublisher {

    public static final String CHANNEL = "dm:message:created";

    private static final Logger log = LoggerFactory.getLogger(DmRedisPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DmRedisPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(DmRedisMessage message) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            log.error("DM Redis Pub/Sub publish failed. messageId={}, conversationId={}",
                    message.messageId(), message.conversationId(), exception);
        }
    }
}
