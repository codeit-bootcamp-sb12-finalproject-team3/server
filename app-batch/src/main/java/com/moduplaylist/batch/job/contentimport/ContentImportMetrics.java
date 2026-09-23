package com.moduplaylist.batch.job.contentimport;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.batch.item.ExecutionContext;

public class ContentImportMetrics {
    private static final int FAILED_ID_LIMIT = 20;

    private long candidateCount;
    private long existingCount;
    private long createdCount;
    private long sportUpdatedCount;
    private long missingRequiredCount;
    private long freeLimitHitCount;
    private final Set<String> failedIds = new LinkedHashSet<>();
    private final Set<String> recordedFailedIds = new LinkedHashSet<>();

    public void candidate() {
        candidateCount++;
    }

    public void existing() {
        existingCount++;
    }

    public void created() {
        createdCount++;
    }

    public void created(long count) {
        createdCount += count;
    }

    public void sportUpdated() {
        sportUpdatedCount++;
    }

    public void missingRequired() {
        missingRequiredCount++;
    }

    public void freeLimitHit() {
        freeLimitHitCount++;
    }

    public void failed(String externalId) {
        if (!failedIds.add(externalId)) return;
        if (recordedFailedIds.size() < FAILED_ID_LIMIT) {
            recordedFailedIds.add(externalId);
        }
    }

    public boolean hasFailures() {
        return !failedIds.isEmpty();
    }

    public void writeTo(ExecutionContext context) {
        context.putLong("candidateCount", candidateCount);
        context.putLong("existingCount", existingCount);
        context.putLong("createdCount", createdCount);
        context.putLong("sportUpdatedCount", sportUpdatedCount);
        context.putLong("missingRequiredCount", missingRequiredCount);
        context.putLong("freeLimitHitCount", freeLimitHitCount);
        context.putLong("failedCount", failedIds.size());
        context.putString("failedIds", String.join(",", recordedFailedIds));
        context.putLong("failedIdsOmittedCount", failedIds.size() - recordedFailedIds.size());
    }

    public String summary() {
        return "candidates=" + candidateCount
            + ", existing=" + existingCount
            + ", created=" + createdCount
            + ", updated=" + sportUpdatedCount
            + ", missing=" + missingRequiredCount
            + ", limitHits=" + freeLimitHitCount
            + ", failed=" + failedIds.size()
            + ", failedIds=[" + String.join(",", recordedFailedIds) + "]"
            + ", omittedFailedIds=" + (failedIds.size() - recordedFailedIds.size());
    }
}
