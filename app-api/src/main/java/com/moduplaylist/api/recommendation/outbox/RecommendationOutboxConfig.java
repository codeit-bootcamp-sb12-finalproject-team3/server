package com.moduplaylist.api.recommendation.outbox;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationOutboxConfig {

    @Bean
    public Clock recommendationOutboxClock() {
        return Clock.systemUTC();
    }
}
