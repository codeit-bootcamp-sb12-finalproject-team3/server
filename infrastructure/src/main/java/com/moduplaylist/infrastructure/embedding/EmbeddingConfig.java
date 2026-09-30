package com.moduplaylist.infrastructure.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
@ConditionalOnProperty(prefix = "mopl.embedding", name = "enabled", havingValue = "true")
public class EmbeddingConfig {

    @Bean
    public EmbeddingGenerator embeddingGenerator(
            EmbeddingModel embeddingModel,
            EmbeddingProperties embeddingProperties
    ) {
        return new SpringAiEmbeddingGenerator(embeddingModel, embeddingProperties);
    }
}
