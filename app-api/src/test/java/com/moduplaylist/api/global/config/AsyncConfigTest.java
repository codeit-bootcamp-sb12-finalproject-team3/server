package com.moduplaylist.api.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    @Test
    void createsBoundedRecommendationPostProcessingExecutor() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AsyncConfig.class)) {
            ThreadPoolTaskExecutor executor = context.getBean(
                    "recommendationPostProcessingExecutor",
                    ThreadPoolTaskExecutor.class
            );

            assertThat(context.containsBean(
                    TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME
            )).isTrue();
            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getMaxPoolSize()).isEqualTo(4);
            assertThat(executor.getThreadNamePrefix())
                    .isEqualTo("recommendation-postprocess-");
            assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity())
                    .isEqualTo(20);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        }
    }
}
