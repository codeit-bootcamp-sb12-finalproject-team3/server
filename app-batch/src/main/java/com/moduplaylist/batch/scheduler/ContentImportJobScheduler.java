package com.moduplaylist.batch.scheduler;

import com.moduplaylist.batch.job.contentimport.ContentImportJobConfig;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "mopl.batch.content-import.scheduler", name = "enabled", havingValue = "true")
public class ContentImportJobScheduler {
    private static final int MAX_EXECUTION_COUNT = 2;
    private static final ZoneId CONTENT_IMPORT_ZONE = ZoneId.of("Asia/Seoul");

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job job;
    private final Clock clock;

    public ContentImportJobScheduler(JobLauncher jobLauncher, JobExplorer jobExplorer,
        @Qualifier(ContentImportJobConfig.JOB_NAME) Job job) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.job = job;
        this.clock = Clock.system(CONTENT_IMPORT_ZONE);
    }

    @Scheduled(cron = "${mopl.batch.content-import.scheduler.cron}",
        zone = "${mopl.batch.content-import.scheduler.zone}")
    public void runContentImportJob() {
        if (!jobExplorer.findRunningJobExecutions(job.getName()).isEmpty()) {
            log.warn("이미 실행 중인 콘텐츠 수집 Job을 건너뜁니다.");
            return;
        }
        String runDate = LocalDate.now(clock).toString();
        JobParameters parameters = new JobParametersBuilder()
            .addString(ContentImportJobConfig.RUN_DATE_PARAMETER, runDate)
            .toJobParameters();
        launch(parameters, runDate, "정기 실행");
    }

    @Scheduled(cron = "${mopl.batch.content-import.scheduler.retry-cron:0 30 * * * *}",
        zone = "${mopl.batch.content-import.scheduler.zone}")
    public void retryFailedContentImportJob() {
        if (!jobExplorer.findRunningJobExecutions(job.getName()).isEmpty()) {
            return;
        }
        String runDate = LocalDate.now(clock).toString();
        JobParameters parameters = new JobParametersBuilder()
            .addString(ContentImportJobConfig.RUN_DATE_PARAMETER, runDate)
            .toJobParameters();
        JobInstance jobInstance = jobExplorer.getJobInstance(job.getName(), parameters);
        JobExecution lastExecution = jobInstance == null
            ? null
            : jobExplorer.getLastJobExecution(jobInstance);
        if (lastExecution == null
            || lastExecution.getStatus() != BatchStatus.FAILED
            || lastExecution.getEndTime() == null) {
            return;
        }
        int executionCount = jobExplorer.getJobExecutions(lastExecution.getJobInstance()).size();
        if (executionCount >= MAX_EXECUTION_COUNT) {
            return;
        }
        launch(parameters, runDate, "자동 재시작");
    }

    private void launch(JobParameters parameters, String runDate, String trigger) {
        try {
            JobExecution execution = jobLauncher.run(job, parameters);
            log.info("콘텐츠 수집 Job {} - runDate={}, executionId={}, status={}",
                trigger, runDate, execution.getId(), execution.getStatus());
        } catch (JobExecutionAlreadyRunningException exception) {
            log.info("동일 실행일의 콘텐츠 수집 Job이 이미 실행 중입니다. runDate={}", runDate);
        } catch (JobInstanceAlreadyCompleteException exception) {
            log.info("동일 실행일의 콘텐츠 수집 Job이 이미 완료됐습니다. runDate={}", runDate);
        } catch (Exception exception) {
            log.error("콘텐츠 수집 Job 실행 실패 - runDate={}", runDate, exception);
        }
    }
}
