package com.moduplaylist.api.recommendation.outbox;

import com.moduplaylist.api.recommendation.event.InitialPreferenceCreatedEvent;
import com.moduplaylist.api.recommendation.service.InitialPreferencePostProcessingService;
import com.moduplaylist.api.recommendation.service.RecommendationOutboxClaim;
import com.moduplaylist.api.recommendation.service.RecommendationOutboxStateService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "mopl.recommendation.outbox",
        name = "worker-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RecommendationOutboxWorker {

    private final RecommendationOutboxStateService stateService;
    private final InitialPreferencePostProcessingService postProcessingService;
    private final RecommendationOutboxRetryPolicy retryPolicy;
    private final RecommendationOutboxProperties properties;
    private final Clock clock;

    public RecommendationOutboxWorker(
            RecommendationOutboxStateService stateService,
            InitialPreferencePostProcessingService postProcessingService,
            RecommendationOutboxRetryPolicy retryPolicy,
            RecommendationOutboxProperties properties,
            @Qualifier("recommendationOutboxClock") Clock clock
    ) {
        this.stateService = stateService;
        this.postProcessingService = postProcessingService;
        this.retryPolicy = retryPolicy;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${mopl.recommendation.outbox.polling-interval-ms:3000}"
    )
    public void poll() {
        Instant now = clock.instant();
        List<RecommendationOutboxClaim> claims = stateService.claimAvailable(
                properties.getClaimLimit(),
                now,
                properties.getProcessingTimeout()
        );
        claims.forEach(this::process);
    }

    private void process(RecommendationOutboxClaim claim) {
        log.info(
                "Outbox 초기 선호 후처리를 시작합니다. "
                        + "eventId={}, userId={}, claimToken={}",
                claim.eventId(),
                claim.userId(),
                claim.claimToken()
        );
        try {
            postProcessingService.process(new InitialPreferenceCreatedEvent(
                    claim.eventId(),
                    claim.userId(),
                    claim.createdAt()
            ));
            if (stateService.complete(claim.id(), claim.claimToken(), clock.instant())) {
                log.info(
                        "Outbox 초기 선호 후처리를 완료했습니다. "
                                + "eventId={}, userId={}, claimToken={}",
                        claim.eventId(),
                        claim.userId(),
                        claim.claimToken()
                );
            } else {
                log.warn(
                        "완료 상태 갱신 권한을 잃었습니다. eventId={}, userId={}",
                        claim.eventId(),
                        claim.userId()
                );
            }
        } catch (RuntimeException exception) {
            handleFailure(claim, exception);
        }
    }

    private void handleFailure(
            RecommendationOutboxClaim claim,
            RuntimeException exception
    ) {
        String lastError = exception.getClass().getSimpleName()
                + ": " + exception.getMessage();
        Instant failedAt = clock.instant();
        try {
            boolean updated = retryPolicy.nextRetryAt(claim.retryCount(), failedAt)
                    .map(nextRetryAt -> scheduleRetry(
                            claim,
                            failedAt,
                            nextRetryAt,
                            lastError
                    ))
                    .orElseGet(() -> markFailed(claim, failedAt, lastError));
            if (!updated) {
                log.warn(
                        "실패 상태 갱신 권한을 잃었습니다. eventId={}, userId={}",
                        claim.eventId(),
                        claim.userId()
                );
            }
        } catch (RuntimeException stateUpdateException) {
            log.error(
                    "Outbox 실패 상태를 저장하지 못했습니다. eventId={}, userId={}",
                    claim.eventId(),
                    claim.userId(),
                    stateUpdateException
            );
        }
    }

    private boolean scheduleRetry(
            RecommendationOutboxClaim claim,
            Instant failedAt,
            Instant nextRetryAt,
            String lastError
    ) {
        boolean scheduled = stateService.retry(
                claim.id(),
                claim.claimToken(),
                failedAt,
                nextRetryAt,
                lastError
        );
        if (scheduled) {
            log.warn(
                    "Outbox 초기 선호 후처리를 재시도합니다. "
                            + "eventId={}, userId={}, retryCount={}, nextRetryAt={}",
                    claim.eventId(),
                    claim.userId(),
                    claim.retryCount() + 1,
                    nextRetryAt
            );
        }
        return scheduled;
    }

    private boolean markFailed(
            RecommendationOutboxClaim claim,
            Instant failedAt,
            String lastError
    ) {
        boolean failed = stateService.fail(
                claim.id(),
                claim.claimToken(),
                failedAt,
                lastError
        );
        if (failed) {
            log.error(
                    "Outbox 초기 선호 후처리 재시도를 소진했습니다. "
                            + "eventId={}, userId={}, retryCount={}",
                    claim.eventId(),
                    claim.userId(),
                    claim.retryCount()
            );
        }
        return failed;
    }
}
