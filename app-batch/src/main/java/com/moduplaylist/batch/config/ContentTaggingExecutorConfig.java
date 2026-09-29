package com.moduplaylist.batch.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConditionalOnProperty(prefix = "mopl.batch.content-tagging", name = "enabled", havingValue = "true")
public class ContentTaggingExecutorConfig {

    public static final String EXECUTOR_NAME = "contentTaggingExecutor";

    @Bean(name = EXECUTOR_NAME)
    public ThreadPoolTaskExecutor contentTaggingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("content-tagging-");
        return executor;
    }
}
