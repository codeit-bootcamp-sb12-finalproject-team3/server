package com.moduplaylist.api.recommendation.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.moduplaylist.api.recommendation.service.InitialPreferencePostProcessingService;
import com.moduplaylist.api.recommendation.service.RecommendationOutboxStateService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class RecommendationOutboxCutoverTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void enablesOnlyOutboxWorkerByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RecommendationOutboxWorker.class);
        });
    }

    @Test
    void disablesWorkerWhenExplicitlyConfigured() {
        contextRunner
                .withPropertyValues(
                        "mopl.recommendation.outbox.worker-enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            RecommendationOutboxWorker.class
                    );
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(RecommendationOutboxWorker.class)
    static class TestConfig {

        @Bean
        RecommendationOutboxStateService stateService() {
            return mock(RecommendationOutboxStateService.class);
        }

        @Bean
        InitialPreferencePostProcessingService postProcessingService() {
            return mock(InitialPreferencePostProcessingService.class);
        }

        @Bean
        RecommendationOutboxRetryPolicy retryPolicy() {
            return new RecommendationOutboxRetryPolicy();
        }

        @Bean
        RecommendationOutboxProperties properties() {
            return new RecommendationOutboxProperties();
        }

        @Bean("recommendationOutboxClock")
        Clock recommendationOutboxClock() {
            return Clock.systemUTC();
        }
    }
}
