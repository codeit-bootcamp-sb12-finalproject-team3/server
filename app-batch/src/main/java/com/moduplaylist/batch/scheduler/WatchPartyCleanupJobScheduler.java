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
        prefix = "mopl.batch.watch-party-cleanup.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class WatchPartyCleanupJobScheduler {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job watchPartyCleanupJob;

    public WatchPartyCleanupJobScheduler(
            JobLauncher jobLauncher,
            JobExplorer jobExplorer,
            @Qualifier("watchPartyCleanupJob") Job watchPartyCleanupJob
    ) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.watchPartyCleanupJob = watchPartyCleanupJob;
    }

    @Scheduled(
            cron = "${mopl.batch.watch-party-cleanup.scheduler.cron}",
            zone = "${mopl.batch.watch-party-cleanup.scheduler.zone}"
    )
    public void runWatchPartyCleanupJob() {
        if (!jobExplorer.findRunningJobExecutions(watchPartyCleanupJob.getName()).isEmpty()) {
            log.warn("이미 실행 중인 종료 Watch Party 정리 배치 Job을 건너뜁니다.");
            return;
        }

        JobParameters parameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(watchPartyCleanupJob, parameters);
            log.info(
                    "종료 Watch Party 정리 배치 Job 실행 - executionId={}, status={}",
                    execution.getId(),
                    execution.getStatus()
            );
        } catch (Exception exception) {
            log.error("종료 Watch Party 정리 배치 Job 실행 실패", exception);
        }
    }
}