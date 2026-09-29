package com.moduplaylist.batch.job.contenttagging;

import com.moduplaylist.core.content.ai.ContentTagGenerator;
import com.moduplaylist.core.content.ai.ContentTaggingException;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.ai.content.SpringAiContentTagGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.PageRequest;

@Slf4j
@RequiredArgsConstructor
public class ContentTaggingTasklet implements Tasklet {
    private static final int PROVIDER_FAILURE_CIRCUIT_THRESHOLD = 3;

    private final ContentRepository contents;
    private final TmdbKeywordService keywords;
    private final ContentTaggingStore store;
    private final ContentTagGenerator generator;
    private final ContentTagGuard guard;
    private final ContentTaggingProperties properties;
    private final ContentIndexSynchronizer indexSynchronizer;
    private final ContentTaggingOpenAiCircuitBreaker circuitBreaker;
    private final AtomicBoolean running = new AtomicBoolean();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext context) {
        if (!running.compareAndSet(false, true)) throw new IllegalStateException("Tagging is already running");
        int succeeded = 0, empty = 0, failed = 0, processed = 0;
        int consecutiveProviderFailures = 0;
        String probeToken = context == null ? null : context.getStepContext()
            .getStepExecution().getJobParameters().getString("circuitProbeToken");
        boolean probe = probeToken != null;
        boolean probeResolved = false;
        int maxItems = probe ? 1 : properties.getMaxItems();
        UUID afterId = null;
        try {
            while (processed < maxItems) {
                var ids = contents.findPendingTaggingIds(List.of(ContentType.MOVIE, ContentType.TV_SEASON),
                    Content.AiTaggingStatus.PENDING, afterId,
                    PageRequest.of(0, Math.min(100, maxItems - processed)));
                if (ids.isEmpty()) {
                    if (probe) {
                        circuitBreaker.probeSucceeded(probeToken);
                        probeResolved = true;
                    }
                    break;
                }
                for (UUID id : ids) {
                    if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Tagging interrupted");
                    afterId = id;
                    processed++;
                    keywords.ensureKeywords(id);
                    var snapshot = store.load(id);
                    if (snapshot == null) continue;
                    long started = System.nanoTime();
                    int calls = 0;
                    boolean retryUsed = false, repair = false;
                    List<String> verified = List.of();
                    ContentTaggingException failure = null;
                    while (true) {
                        try {
                            calls++;
                            verified = guard.validate(generator.generate(snapshot.input(), repair), snapshot.input());
                            break;
                        } catch (ContentTaggingException exception) {
                            if (!retryUsed && exception.isRetryable() && !exception.isAbortJob()) {
                                retryUsed = true;
                                repair = "INVALID_JSON".equals(exception.getMessage())
                                    || "INCOMPLETE_RESPONSE".equals(exception.getMessage())
                                    || "EMPTY_RESPONSE".equals(exception.getMessage());
                                awaitRetry(exception.getRetryAfterMillis());
                                continue;
                            }
                            failure = exception;
                            break;
                        }
                    }
                    long elapsed = (System.nanoTime() - started) / 1_000_000;
                    boolean providerFailure = failure != null
                        && failure.getMessage().startsWith("AI_API_");
                    consecutiveProviderFailures = providerFailure
                        ? consecutiveProviderFailures + 1 : 0;

                    if (providerFailure) {
                        boolean permanentFailure = isPermanentProviderFailure(failure);
                        log.warn("OpenAI 태깅 공급자 장애 contentId={}, reason={}, model={}, prompt={}, calls={}, elapsedMs={}",
                            id, failure.getMessage(), generator.modelName(), SpringAiContentTagGenerator.PROMPT_VERSION,
                            calls, elapsed);
                        if (probe) {
                            circuitBreaker.probeFailed(probeToken, permanentFailure);
                            probeResolved = true;
                            return RepeatStatus.FINISHED;
                        }
                        if (failure.isAbortJob()
                            || consecutiveProviderFailures >= PROVIDER_FAILURE_CIRCUIT_THRESHOLD) {
                            circuitBreaker.openAfterConsecutiveFailures(permanentFailure);
                            return RepeatStatus.FINISHED;
                        }
                        continue;
                    }

                    boolean completed = save(id, snapshot, failure == null ? verified : List.of());
                    if (!completed) {
                        log.info("콘텐츠 태깅 보류 contentId={}, reason=SOURCE_CHANGED", id);
                        continue;
                    }
                    try {
                        // The tag transaction has committed; OpenSearch never runs while holding the content lock.
                        indexSynchronizer.synchronize(id);
                    } catch (RuntimeException exception) {
                        // embedding_pending remains durable and the embedding job repairs both indexes.
                        log.warn("태깅 후 색인 보류 contentId={}, reason={}",
                            id, exception.getClass().getSimpleName());
                    }
                    if (failure != null) {
                        failed++;
                        log.warn("콘텐츠 태깅 실패 contentId={}, reason={}, model={}, prompt={}, calls={}, elapsedMs={}",
                            id, failure.getMessage(), generator.modelName(), SpringAiContentTagGenerator.PROMPT_VERSION,
                            calls, elapsed);
                    } else {
                        if (verified.isEmpty()) empty++; else succeeded++;
                        log.info("콘텐츠 태깅 종료 contentId={}, result={}, count={}, model={}, prompt={}, calls={}, elapsedMs={}",
                            id, verified.isEmpty() ? "NO_USABLE_TAGS" : "SUCCESS", verified.size(),
                            generator.modelName(), SpringAiContentTagGenerator.PROMPT_VERSION, calls, elapsed);
                    }
                    if (probe) {
                        circuitBreaker.probeSucceeded(probeToken);
                        probeResolved = true;
                        return RepeatStatus.FINISHED;
                    }
                }
            }
            return RepeatStatus.FINISHED;
        } finally {
            if (probe && !probeResolved) circuitBreaker.probeAborted(probeToken);
            running.set(false);
            log.info("콘텐츠 태깅 배치 종료 성공={}, 정상0개={}, 실제실패={}", succeeded, empty, failed);
        }
    }

    private boolean save(UUID id, ContentTaggingStore.Snapshot snapshot, List<String> tags) {
        try {
            return store.complete(id, snapshot, tags);
        } catch (TransientDataAccessException exception) {
            // A fresh short transaction; reuse verified tags, not another model request.
            return store.complete(id, snapshot, tags);
        }
    }

    private static void awaitRetry(long retryAfterMillis) {
        try {
            Thread.sleep(Math.max(retryAfterMillis, 500 + ThreadLocalRandom.current().nextLong(250)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tagging interrupted");
        }
    }

    private static boolean isPermanentProviderFailure(ContentTaggingException failure) {
        return switch (failure.getMessage()) {
            case "AI_API_STATUS_400", "AI_API_STATUS_401", "AI_API_STATUS_403",
                 "AI_API_STATUS_404" -> true;
            default -> false;
        };
    }
}
