package com.moduplaylist.batch.scheduler;

import com.moduplaylist.batch.config.ContentTaggingExecutorConfig;
import com.moduplaylist.batch.job.contenttagging.ContentTaggingOpenAiCircuitBreaker;
import com.moduplaylist.batch.job.contenttagging.ContentTaggingJobConfig;
import java.time.LocalTime;
import java.time.ZoneId;
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
    private static final ZoneId TAGGING_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime TAGGING_WINDOW_END = LocalTime.of(3, 0);
    private final JobLauncher launcher;
    private final JobExplorer explorer;
    private final Job job;
    private final TaskExecutor executor;
    private final ContentTaggingOpenAiCircuitBreaker circuitBreaker;
    private final AtomicBoolean requested = new AtomicBoolean();

    public ContentTaggingJobScheduler(JobLauncher launcher, JobExplorer explorer,
        @Qualifier(ContentTaggingJobConfig.JOB_NAME) Job job,
        @Qualifier(ContentTaggingExecutorConfig.EXECUTOR_NAME) TaskExecutor executor,
        ContentTaggingOpenAiCircuitBreaker circuitBreaker) {
        this.launcher = launcher;
        this.explorer = explorer;
        this.job = job;
        this.executor = executor;
        this.circuitBreaker = circuitBreaker;
    }

    @Scheduled(cron = "${mopl.batch.content-tagging.cron:0 * * * * *}", zone = "Asia/Seoul")
    public void run() {
        submit("scheduled", null);
    }

    public void runAfterContentImport(long contentImportExecutionId) {
        if (!withinTaggingWindow(LocalTime.now(TAGGING_ZONE))) {
            log.info("콘텐츠 태깅 운영 시간 밖이므로 수집 후 실행을 건너뜁니다. executionId={}",
                contentImportExecutionId);
            return;
        }
        submit("content-import", contentImportExecutionId);
    }

    static boolean withinTaggingWindow(LocalTime time) {
        return time.isBefore(TAGGING_WINDOW_END);
    }

    private void submit(String trigger, Long contentImportExecutionId) {
        if (!requested.compareAndSet(false, true)) {
            log.debug("콘텐츠 태깅 실행 중이므로 요청을 건너뜁니다. trigger={}", trigger);
            return;
        }
        ContentTaggingOpenAiCircuitBreaker.Permit permit;
        try {
            permit = circuitBreaker.acquire();
        } catch (RuntimeException exception) {
            requested.set(false);
            log.warn("OpenAI 태깅 circuit 상태를 확인하지 못해 요청을 건너뜁니다. trigger={}", trigger,
                exception);
            return;
        }
        if (!permit.allowed()) {
            requested.set(false);
            log.debug("OpenAI 태깅 circuit이 열려 요청을 건너뜁니다. trigger={}", trigger);
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    launch(trigger, contentImportExecutionId, permit);
                } finally {
                    requested.set(false);
                }
            });
        } catch (TaskRejectedException exception) {
            requested.set(false);
            if (permit.probe()) circuitBreaker.probeAborted(permit.token());
            log.warn("콘텐츠 태깅 실행 요청이 거절됐습니다. trigger={}", trigger);
        }
    }

    private void launch(String trigger, Long contentImportExecutionId,
                        ContentTaggingOpenAiCircuitBreaker.Permit permit) {
        if (!explorer.findRunningJobExecutions(job.getName()).isEmpty()) {
            if (permit.probe()) circuitBreaker.probeAborted(permit.token());
            return;
        }
        try {
            JobParametersBuilder parameters = new JobParametersBuilder()
                .addLong("requestedAt", System.currentTimeMillis())
                .addString("trigger", trigger);
            if (permit.probe()) {
                parameters.addString("circuitProbeToken", permit.token());
            }
            if (contentImportExecutionId != null) {
                parameters.addLong("contentImportExecutionId", contentImportExecutionId);
            }
            launcher.run(job, parameters.toJobParameters());
        } catch (Exception exception) {
            if (permit.probe()) circuitBreaker.probeAborted(permit.token());
            log.error("콘텐츠 태깅 Job 실행 실패 reason={}", exception.getClass().getSimpleName());
        }
    }
}
