package com.moduplaylist.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.infrastructure.tmdb.TmdbKrWatchProviderExtractor;
import com.moduplaylist.infrastructure.tmdb.TmdbContentClient;
import com.moduplaylist.infrastructure.tmdb.TmdbProperties;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderClient;
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

    @Bean
    public TmdbWatchProviderClient tmdbWatchProviderClient(
        HttpClient tmdbHttpClient,
        ObjectMapper objectMapper,
        TmdbProperties properties
    ) {
        return new TmdbWatchProviderClient(tmdbHttpClient, objectMapper, properties);
    }

    @Bean
    public TmdbContentClient tmdbContentClient(
        HttpClient tmdbHttpClient,
        ObjectMapper objectMapper,
        TmdbProperties properties
    ) {
        return new TmdbContentClient(tmdbHttpClient, objectMapper, properties);
    }

    @Bean
    public TmdbKrWatchProviderExtractor tmdbKrWatchProviderExtractor() {
        return new TmdbKrWatchProviderExtractor();
    }
}
