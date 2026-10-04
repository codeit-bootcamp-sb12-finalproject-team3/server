package com.moduplaylist.realtime.global.config;

import com.moduplaylist.realtime.notification.redis.NotificationRedisMessageListener;
import com.moduplaylist.realtime.notification.redis.NotificationRedisPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class NotificationRedisConfig {

    @Bean
    public RedisMessageListenerContainer notificationListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationRedisMessageListener notificationRedisMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                notificationRedisMessageListener,
                new ChannelTopic(NotificationRedisPublisher.CHANNEL)
        );
        return container;
    }
}
