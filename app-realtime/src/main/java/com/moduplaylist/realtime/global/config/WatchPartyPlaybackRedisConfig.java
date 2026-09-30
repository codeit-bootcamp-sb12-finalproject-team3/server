package com.moduplaylist.realtime.global.config;

import com.moduplaylist.realtime.watchparty.redis.WatchPartyPlaybackMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class WatchPartyPlaybackRedisConfig {

    @Bean
    public RedisMessageListenerContainer watchPartyPlaybackListenerContainer(
            RedisConnectionFactory connectionFactory,
            WatchPartyPlaybackMessageListener watchPartyPlaybackMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(watchPartyPlaybackMessageListener, new PatternTopic("watchparty:*:playback"));
        return container;
    }
}