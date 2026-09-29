package com.moduplaylist.api.recommendation.event;

import com.moduplaylist.api.recommendation.metric.InitialPreferencePostProcessingMetrics;
import com.moduplaylist.api.recommendation.service.InitialPreferencePostProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "mopl.recommendation.outbox",
        name = "worker-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InitialPreferenceCreatedEventListener {

    private final InitialPreferencePostProcessingService postProcessingService;
    private final InitialPreferencePostProcessingMetrics metrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InitialPreferenceCreatedEvent event) {
        try {
            postProcessingService.processAsync(event);
        } catch (TaskRejectedException exception) {
            metrics.recordRejected();
            log.error(
                    "초기 선호 추천 후처리 작업이 거절되었습니다. eventId={}, userId={}",
                    event.eventId(),
                    event.userId(),
                    exception
            );
        }
    }
}
