package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentEmbeddingRunWindowService {

    private static final int HISTORY_PAGE_SIZE = 100;

    private final JobExplorer jobExplorer;
    private final EmbeddingGenerator embeddingGenerator;

    public ContentEmbeddingRunWindow forExecution(JobExecution currentExecution) {
        Long requestedAt = currentExecution.getJobParameters().getLong("requestedAt");
        Instant through = requestedAt == null
                ? Instant.now()
                : Instant.ofEpochMilli(requestedAt);

        if (Boolean.parseBoolean(currentExecution.getJobParameters().getString("fullScan"))) {
            return new ContentEmbeddingRunWindow(through, true);
        }

        Optional<JobExecution> previous = findLastCompletedExecution();
        if (previous.isEmpty()) {
            return new ContentEmbeddingRunWindow(through, true);
        }

        JobExecution lastCompleted = previous.get();
        String lastModel = lastCompleted.getJobParameters().getString("embeddingModel");
        if (!embeddingGenerator.modelName().equals(lastModel)) {
            return new ContentEmbeddingRunWindow(through, true);
        }
        return new ContentEmbeddingRunWindow(through, false);
    }

    private Optional<JobExecution> findLastCompletedExecution() {
        for (int offset = 0; ; offset += HISTORY_PAGE_SIZE) {
            var instances = jobExplorer.getJobInstances(
                    ContentEmbeddingJobConfig.JOB_NAME, offset, HISTORY_PAGE_SIZE
            );
            if (instances.isEmpty()) {
                return Optional.empty();
            }

            Optional<JobExecution> completed = instances.stream()
                    .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                    .filter(execution -> execution.getStatus() == BatchStatus.COMPLETED)
                    .max(Comparator.comparing(JobExecution::getId));
            if (completed.isPresent()) {
                return completed;
            }
        }
    }
}
