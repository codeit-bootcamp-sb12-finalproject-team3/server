package com.moduplaylist.batch.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "mopl.batch.embedding.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class EmbeddingJobScheduler {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job contentEmbeddingJob;
    private final Job userProfileEmbeddingJob;

    public EmbeddingJobScheduler(
            JobLauncher jobLauncher,
            JobExplorer jobExplorer,
            @Qualifier("contentEmbeddingJob") Job contentEmbeddingJob,
            @Qualifier("userProfileEmbeddingJob") Job userProfileEmbeddingJob
    ) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.contentEmbeddingJob = contentEmbeddingJob;
        this.userProfileEmbeddingJob = userProfileEmbeddingJob;
    }

    @Scheduled(
            cron = "${mopl.batch.embedding.scheduler.content-cron}",
            zone = "${mopl.batch.embedding.scheduler.zone}"
    )
    public void runContentEmbeddingJob() {
        launch(contentEmbeddingJob);
    }

    @Scheduled(
            cron = "${mopl.batch.embedding.scheduler.user-profile-cron}",
            zone = "${mopl.batch.embedding.scheduler.zone}"
    )
    public void runUserProfileEmbeddingJob() {
        launch(userProfileEmbeddingJob);
    }

    private void launch(Job job) {
        if (!jobExplorer.findRunningJobExecutions(job.getName()).isEmpty()) {
            log.warn("이미 실행 중인 배치 Job을 건너뜁니다. job={}", job.getName());
            return;
        }

        JobParameters parameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
        try {
            JobExecution execution = jobLauncher.run(job, parameters);
            log.info(
                    "임베딩 배치 Job 실행 - job={}, executionId={}, status={}",
                    job.getName(),
                    execution.getId(),
                    execution.getStatus()
            );
        } catch (Exception exception) {
            log.error("임베딩 배치 Job 실행 실패 - job={}", job.getName(), exception);
        }
    }
}
