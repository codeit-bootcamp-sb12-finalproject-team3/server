package com.moduplaylist.api.recommendation.event;

import com.moduplaylist.api.recommendation.service.RecommendationRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.embedding", name = "enabled", havingValue = "true")
public class PreferenceChangedEventListener {

    private final RecommendationRefreshService refreshService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PreferenceChangedEvent event) {
        try {
            refreshService.recordActivity(event.userId(), event.appliedDelta());
        } catch (RuntimeException exception) {
            log.error(
                    "실시간 추천 갱신 처리에 실패했습니다. eventId={}, userId={}, appliedDelta={}",
                    event.eventId(),
                    event.userId(),
                    event.appliedDelta(),
                    exception
            );
        }
    }
}
