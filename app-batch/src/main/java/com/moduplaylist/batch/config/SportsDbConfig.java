package com.moduplaylist.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.infrastructure.sportsdb.SportsDbClient;
import com.moduplaylist.infrastructure.sportsdb.SportsDbProperties;
import com.moduplaylist.batch.job.contentimport.SportsImportProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SportsDbProperties.class, SportsImportProperties.class})
public class SportsDbConfig {
    @Bean
    public SportsDbClient sportsDbClient(ObjectMapper objectMapper, SportsDbProperties properties) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .build();
        return new SportsDbClient(client, objectMapper, properties);
    }
}
