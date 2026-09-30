package com.moduplaylist.batch.job.contentimport;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

@RequiredArgsConstructor
public class ContentImportMetricsStepListener implements StepExecutionListener {
    private static final String ITEM_METRIC = "mopl.batch.content.import.items";
    private static final String FREE_LIMIT_HIT_METRIC =
        "mopl.batch.content.import.free.limit.hits";
    private static final String SPORTS_SOURCE = "THESPORTSDB";

    private final MeterRegistry meterRegistry;
    private final String source;

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ExecutionContext context = stepExecution.getExecutionContext();
        incrementItem(context, "candidate", ContentImportMetrics.CANDIDATE_COUNT);
        incrementItem(context, "existing", ContentImportMetrics.EXISTING_COUNT);
        incrementItem(context, "created", ContentImportMetrics.CREATED_COUNT);
        incrementItem(context, "updated", ContentImportMetrics.SPORT_UPDATED_COUNT);
        incrementItem(context, "missing", ContentImportMetrics.MISSING_REQUIRED_COUNT);
        incrementItem(context, "failed", ContentImportMetrics.FAILED_COUNT);
        if (SPORTS_SOURCE.equals(source)) {
            meterRegistry.counter(FREE_LIMIT_HIT_METRIC, "source", source)
                .increment(context.getLong(ContentImportMetrics.FREE_LIMIT_HIT_COUNT, 0L));
        }
        return null;
    }

    private void incrementItem(ExecutionContext context, String result, String contextKey) {
        meterRegistry.counter(ITEM_METRIC, "source", source, "result", result)
            .increment(context.getLong(contextKey, 0L));
    }
}
