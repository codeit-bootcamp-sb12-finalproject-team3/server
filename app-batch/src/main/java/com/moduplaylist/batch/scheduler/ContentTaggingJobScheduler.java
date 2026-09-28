package com.moduplaylist.batch.scheduler;

import com.moduplaylist.batch.config.ContentTaggingExecutorConfig;
import com.moduplaylist.batch.job.contenttagging.ContentTaggingJobConfig;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Enable on exactly one batch instance. This guard is not a distributed lease. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mopl.batch.content-tagging", name = "enabled", havingValue = "true")
public class ContentTaggingJobScheduler {
    private final JobLauncher launcher;
    private final JobExplorer explorer;
    private final Job job;
    private final TaskExecutor executor;
    private final AtomicBoolean requested = new AtomicBoolean();

    public ContentTaggingJobScheduler(JobLauncher launcher, JobExplorer explorer,
        @Qualifier(ContentTaggingJobConfig.JOB_NAME) Job job,
        @Qualifier(ContentTaggingExecutorConfig.EXECUTOR_NAME) TaskExecutor executor) {
        this.launcher = launcher;
        this.explorer = explorer;
        this.job = job;
        this.executor = executor;
    }

    @Scheduled(cron = "${mopl.batch.content-tagging.cron:0 * * * * *}", zone = "Asia/Seoul")
    public void run() {
        submit("scheduled", null);
    }

    public void runAfterContentImport(long contentImportExecutionId) {
        submit("content-import", contentImportExecutionId);
    }

    private void submit(String trigger, Long contentImportExecutionId) {
        if (!requested.compareAndSet(false, true)) {
            log.debug("콘텐츠 태깅 실행 중이므로 요청을 건너뜁니다. trigger={}", trigger);
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    launch(trigger, contentImportExecutionId);
                } finally {
                    requested.set(false);
                }
            });
        } catch (TaskRejectedException exception) {
            requested.set(false);
            log.warn("콘텐츠 태깅 실행 요청이 거절됐습니다. trigger={}", trigger);
        }
    }

    private void launch(String trigger, Long contentImportExecutionId) {
        if (!explorer.findRunningJobExecutions(job.getName()).isEmpty()) return;
        try {
            JobParametersBuilder parameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("trigger", trigger);
            if (contentImportExecutionId != null) {
                parameters.addLong("contentImportExecutionId", contentImportExecutionId);
            }
            launcher.run(job, parameters.toJobParameters());
        } catch (Exception exception) {
            log.error("콘텐츠 태깅 Job 실행 실패 reason={}", exception.getClass().getSimpleName());
        }
    }
}
