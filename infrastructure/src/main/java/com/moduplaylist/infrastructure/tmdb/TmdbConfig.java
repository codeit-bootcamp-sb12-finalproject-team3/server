package com.moduplaylist.infrastructure.tmdb;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

    @Bean
    public HttpClient tmdbHttpClient(TmdbProperties properties) {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .build();
    }
}
