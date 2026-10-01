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
import ch.qos.logback.core.read.ListAppender;
import com.moduplaylist.api.recommendation.metric.InitialPreferencePostProcessingMetrics;
import com.moduplaylist.infrastructure.recommendation.ContentRecommendationService;
import com.moduplaylist.infrastructure.recommendation.PlaylistRecommendationService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserContentProfileEmbeddingService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserPlaylistProfileEmbeddingService;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class InitialPreferencePostProcessingServiceTest {

    private static final UUID EVENT_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001"
    );
    private static final UUID USER_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000002"
    );

    @Test
    void processesAllStagesInOrderOutsideTransaction() {
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
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        doAnswer(invocation -> {
            transactionActive.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            return null;
        }).when(contentEmbeddingService).embedAndIndex(USER_ID);
        InitialPreferencePostProcessingService service = service(
                contentEmbeddingService,
                playlistEmbeddingService,
                contentRecommendationService,
                playlistRecommendationService,
                metrics
        );

        service.process(EVENT_ID, USER_ID);

        InOrder order = inOrder(
                contentEmbeddingService,
                playlistEmbeddingService,
                contentRecommendationService,
                playlistRecommendationService
        );
        order.verify(contentEmbeddingService).embedAndIndex(USER_ID);
        order.verify(playlistEmbeddingService).embedAndIndex(USER_ID);
        order.verify(contentRecommendationService).generateAndCache(USER_ID);
        order.verify(playlistRecommendationService).generateAndCache(USER_ID);
        verify(metrics).recordSuccess();
        verify(metrics).recordDuration(any(Duration.class));
        assertThat(transactionActive).isFalse();
    }

    @Test
    void stopsAtFailedStageAndLogsItsName() {
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
        when(contentRecommendationService.generateAndCache(USER_ID))
                .thenThrow(new IllegalStateException("content recommendation failed"));
        InitialPreferencePostProcessingService service = service(
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
            assertThatThrownBy(() -> service.process(EVENT_ID, USER_ID))
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
                                    + "eventId=" + EVENT_ID + ", "
                                    + "userId=" + USER_ID + ", durationMs="
                    ));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLogLevel);
            appender.stop();
        }
    }

    private InitialPreferencePostProcessingService service(
            UserContentProfileEmbeddingService contentEmbeddingService,
            UserPlaylistProfileEmbeddingService playlistEmbeddingService,
            ContentRecommendationService contentRecommendationService,
            PlaylistRecommendationService playlistRecommendationService,
            InitialPreferencePostProcessingMetrics metrics
    ) {
        return new InitialPreferencePostProcessingService(
                contentEmbeddingService,
                playlistEmbeddingService,
                contentRecommendationService,
                playlistRecommendationService,
                metrics
        );
    }
}
