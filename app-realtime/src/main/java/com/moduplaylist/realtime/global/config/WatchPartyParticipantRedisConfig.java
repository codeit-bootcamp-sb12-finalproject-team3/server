package com.moduplaylist.realtime.global.config;

import com.moduplaylist.realtime.watchparty.redis.WatchPartyParticipantMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class WatchPartyParticipantRedisConfig {

    @Bean
    public RedisMessageListenerContainer watchPartyParticipantListenerContainer(
            RedisConnectionFactory connectionFactory,
            WatchPartyParticipantMessageListener watchPartyParticipantMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(watchPartyParticipantMessageListener,
                new PatternTopic("watchparty:*:participants"));
        return container;
    }
}