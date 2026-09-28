package com.moduplaylist.api.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.read.ListAppender;
import com.moduplaylist.api.global.config.AsyncConfig;
import com.moduplaylist.api.recommendation.event.InitialPreferenceCreatedEvent;
import com.moduplaylist.api.recommendation.metric.InitialPreferencePostProcessingMetrics;
import com.moduplaylist.infrastructure.recommendation.ContentRecommendationService;
import com.moduplaylist.infrastructure.recommendation.PlaylistRecommendationService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserContentProfileEmbeddingService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserPlaylistProfileEmbeddingService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class InitialPreferencePostProcessingServiceTest {

    @Test
    void processesOnRecommendationPostProcessingExecutor() throws InterruptedException {
        InitialPreferenceCreatedEvent event = event();
        UserContentProfileEmbeddingService contentEmbeddingService = mock(
                UserContentProfileEmbeddingService.class
        );
        UserPlaylistProfileEmbeddingService playlistEmbeddingService = mock(
                UserPlaylistProfileEmbeddingService.class
        );
        ContentRecommendationService contentRecommendationService = mock(
                ContentRecommendationService.class
        );
        PlaylistRecommendationService playlistRecommendationService = mock(
                PlaylistRecommendationService.class
        );
        InitialPreferencePostProcessingMetrics metrics = mock(
                InitialPreferencePostProcessingMetrics.class
        );
        AtomicBoolean transactionActiveDuringContentEmbedding = new AtomicBoolean(true);
        AtomicBoolean transactionActiveDuringPlaylistEmbedding = new AtomicBoolean(true);
        AtomicBoolean transactionActiveDuringContentRecommendation = new AtomicBoolean(true);
        AtomicBoolean transactionActiveDuringPlaylistRecommendation = new AtomicBoolean(true);
        doAnswer(invocation -> {
            transactionActiveDuringContentEmbedding.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            return null;
        }).when(contentEmbeddingService).embedAndIndex(event.userId());
        doAnswer(invocation -> {
            transactionActiveDuringPlaylistEmbedding.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            return null;
        }).when(playlistEmbeddingService).embedAndIndex(event.userId());
        doAnswer(invocation -> {
            transactionActiveDuringContentRecommendation.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            return null;
        }).when(contentRecommendationService).generateAndCache(event.userId());
        doAnswer(invocation -> {
            transactionActiveDuringPlaylistRecommendation.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            return null;
        }).when(playlistRecommendationService).generateAndCache(event.userId());
        Logger logger = (Logger) LoggerFactory.getLogger(
                InitialPreferencePostProcessingService.class
        );
        Level previousLogLevel = logger.getLevel();
        CountDownLatch completionLatch = new CountDownLatch(1);
        List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
        AppenderBase<ILoggingEvent> appender = loggingAppender(events, completionLatch);
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.register(
                    AsyncConfig.class,
                    InitialPreferencePostProcessingService.class
            );
            context.getBeanFactory().registerSingleton(
                    "userContentProfileEmbeddingService",
                    contentEmbeddingService
            );
            context.getBeanFactory().registerSingleton(
                    "userPlaylistProfileEmbeddingService",
                    playlistEmbeddingService
            );
            context.getBeanFactory().registerSingleton(
                    "contentRecommendationService",
                    contentRecommendationService
            );
            context.getBeanFactory().registerSingleton(
                    "playlistRecommendationService",
                    playlistRecommendationService
            );
            context.getBeanFactory().registerSingleton(
                    "initialPreferencePostProcessingMetrics",
                    metrics
            );
            context.refresh();
            String callerThreadName = Thread.currentThread().getName();
            InitialPreferencePostProcessingService service = context.getBean(
                    InitialPreferencePostProcessingService.class
            );

            service.processAsync(event);

            assertThat(completionLatch.await(3, TimeUnit.SECONDS)).isTrue();
            InOrder processingOrder = inOrder(
                    contentEmbeddingService,
                    playlistEmbeddingService,
                    contentRecommendationService,
                    playlistRecommendationService
            );
            processingOrder.verify(contentEmbeddingService).embedAndIndex(event.userId());
            processingOrder.verify(playlistEmbeddingService).embedAndIndex(event.userId());
            processingOrder.verify(contentRecommendationService)
                    .generateAndCache(event.userId());
            processingOrder.verify(playlistRecommendationService)
                    .generateAndCache(event.userId());
            verify(metrics).recordSuccess();
            verify(metrics).recordDuration(any(Duration.class));
            assertThat(transactionActiveDuringContentEmbedding).isFalse();
            assertThat(transactionActiveDuringPlaylistEmbedding).isFalse();
            assertThat(transactionActiveDuringContentRecommendation).isFalse();
            assertThat(transactionActiveDuringPlaylistRecommendation).isFalse();
            assertThat(events).hasSize(2);
            assertThat(events)
                    .extracting(ILoggingEvent::getThreadName)
                    .allSatisfy(threadName -> {
                        assertThat(threadName)
                                .startsWith("recommendation-postprocess-")
                                .isNotEqualTo(callerThreadName);
                    });
            assertThat(events.get(0).getFormattedMessage()).isEqualTo(
                    "초기 선호 추천 후처리를 시작합니다. "
                            + "eventId=00000000-0000-0000-0000-000000000001, "
                            + "userId=00000000-0000-0000-0000-000000000002"
            );
            assertThat(events.get(1).getFormattedMessage())
                    .startsWith(
                            "초기 선호 추천 후처리를 완료했습니다. "
                                    + "eventId=00000000-0000-0000-0000-000000000001, "
                                    + "userId=00000000-0000-0000-0000-000000000002, "
                                    + "durationMs="
                    );
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLogLevel);
            appender.stop();
        }
    }

    @Test
    void stopsAtFailedStageAndLogsItsName() {
        InitialPreferenceCreatedEvent event = event();
        UserContentProfileEmbeddingService contentEmbeddingService = mock(
                UserContentProfileEmbeddingService.class
        );
        UserPlaylistProfileEmbeddingService playlistEmbeddingService = mock(
                UserPlaylistProfileEmbeddingService.class
        );
        ContentRecommendationService contentRecommendationService = mock(
                ContentRecommendationService.class
        );
        PlaylistRecommendationService playlistRecommendationService = mock(
                PlaylistRecommendationService.class
        );
        InitialPreferencePostProcessingMetrics metrics = mock(
                InitialPreferencePostProcessingMetrics.class
        );
        when(contentRecommendationService.generateAndCache(event.userId()))
                .thenThrow(new IllegalStateException("content recommendation failed"));
        InitialPreferencePostProcessingService service =
                new InitialPreferencePostProcessingService(
                        contentEmbeddingService,
                        playlistEmbeddingService,
                        contentRecommendationService,
                        playlistRecommendationService,
                        metrics
                );
        Logger logger = (Logger) LoggerFactory.getLogger(
                InitialPreferencePostProcessingService.class
        );
        Level previousLogLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> service.processAsync(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("content recommendation failed");

            verifyNoInteractions(playlistRecommendationService);
            verify(metrics).recordFailure(true);
            verify(metrics).recordDuration(any(Duration.class));
            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(message -> assertThat(message).startsWith(
                            "초기 선호 추천 후처리에 실패했습니다. "
                                    + "stage=content_recommendation, "
                                    + "eventId=00000000-0000-0000-0000-000000000001, "
                                    + "userId=00000000-0000-0000-0000-000000000002, "
                                    + "durationMs="
                    ));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLogLevel);
            appender.stop();
        }
    }

    private AppenderBase<ILoggingEvent> loggingAppender(
            List<ILoggingEvent> events,
            CountDownLatch completionLatch
    ) {
        return new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event) {
                events.add(event);
                if (event.getFormattedMessage().startsWith(
                        "초기 선호 추천 후처리를 완료했습니다."
                )) {
                    completionLatch.countDown();
                }
            }
        };
    }

    private InitialPreferenceCreatedEvent event() {
        return new InitialPreferenceCreatedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-09-28T08:00:00Z")
        );
    }
}
