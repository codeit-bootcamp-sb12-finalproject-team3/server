package com.moduplaylist.api.recommendation.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.moduplaylist.api.recommendation.metric.InitialPreferencePostProcessingMetrics;
import com.moduplaylist.api.recommendation.service.InitialPreferencePostProcessingService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InitialPreferenceCreatedEventListenerTest.TestConfig.class)
class InitialPreferenceCreatedEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private InitialPreferencePostProcessingService postProcessingService;

    @Autowired
    private InitialPreferencePostProcessingMetrics metrics;

    private Logger listenerLogger;
    private Level previousLogLevel;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        reset(postProcessingService, metrics);
        listenerLogger = (Logger) LoggerFactory.getLogger(
                InitialPreferenceCreatedEventListener.class
        );
        previousLogLevel = listenerLogger.getLevel();
        listenerLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        listenerLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        listenerLogger.detachAppender(listAppender);
        listenerLogger.setLevel(previousLogLevel);
        listAppender.stop();
    }

    @Test
    void handlesEventAfterCommit() {
        InitialPreferenceCreatedEvent event = event();

        beginTransactionSynchronization();
        try {
            eventPublisher.publishEvent(event);
            verifyNoInteractions(postProcessingService);
            completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
        } finally {
            clearTransactionSynchronization();
        }

        verify(postProcessingService).processAsync(event);
    }

    @Test
    void doesNotHandleEventAfterRollback() {
        beginTransactionSynchronization();
        try {
            eventPublisher.publishEvent(event());
            completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
        } finally {
            clearTransactionSynchronization();
        }

        verifyNoInteractions(postProcessingService);
    }

    @Test
    void logsRejectedAsyncTaskAfterCommit() {
        InitialPreferenceCreatedEvent event = event();
        doThrow(new TaskRejectedException("executor saturated"))
                .when(postProcessingService)
                .processAsync(event);

        beginTransactionSynchronization();
        try {
            eventPublisher.publishEvent(event);
            completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
        } finally {
            clearTransactionSynchronization();
        }

        verify(postProcessingService).processAsync(event);
        verify(metrics).recordRejected();
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        "초기 선호 추천 후처리 작업이 거절되었습니다. "
                                + "eventId=00000000-0000-0000-0000-000000000001, "
                                + "userId=00000000-0000-0000-0000-000000000002"
                );
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private void completeTransaction(int completionStatus) {
        TransactionSynchronizationUtils.triggerAfterCompletion(completionStatus);
    }

    private void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private InitialPreferenceCreatedEvent event() {
        return new InitialPreferenceCreatedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-09-28T08:00:00Z")
        );
    }

    @Configuration
    @Import(InitialPreferenceCreatedEventListener.class)
    static class TestConfig {

        @Bean
        static TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        InitialPreferencePostProcessingService postProcessingService() {
            return mock(InitialPreferencePostProcessingService.class);
        }

        @Bean
        InitialPreferencePostProcessingMetrics metrics() {
            return mock(InitialPreferencePostProcessingMetrics.class);
        }
    }
}
