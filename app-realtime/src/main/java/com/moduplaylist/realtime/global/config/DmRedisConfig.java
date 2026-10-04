package com.moduplaylist.realtime.global.config;

import com.moduplaylist.realtime.dm.redis.DmRedisMessageListener;
import com.moduplaylist.realtime.dm.redis.DmRedisPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class DmRedisConfig {

    @Bean
    public RedisMessageListenerContainer dmListenerContainer(
            RedisConnectionFactory connectionFactory,
            DmRedisMessageListener dmRedisMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                dmRedisMessageListener,
                new ChannelTopic(DmRedisPublisher.CHANNEL)
        );
        return container;
    }
}
