package com.moduplaylist.batch.scheduler;

import com.moduplaylist.batch.job.contentimport.ContentImportJobConfig;
import java.time.Clock;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
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
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job job;
    private final Clock clock;

    public ContentImportJobScheduler(JobLauncher jobLauncher, JobExplorer jobExplorer,
        @Qualifier(ContentImportJobConfig.JOB_NAME) Job job) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.job = job;
        this.clock = Clock.systemUTC();
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
        try {
            JobExecution execution = jobLauncher.run(job, parameters);
            log.info("콘텐츠 수집 Job 실행 - runDate={}, executionId={}, status={}",
                runDate, execution.getId(), execution.getStatus());
        } catch (JobExecutionAlreadyRunningException exception) {
            log.info("동일 실행일의 콘텐츠 수집 Job이 이미 실행 중입니다. runDate={}", runDate);
        } catch (JobInstanceAlreadyCompleteException exception) {
            log.info("동일 실행일의 콘텐츠 수집 Job이 이미 완료됐습니다. runDate={}", runDate);
        } catch (Exception exception) {
            log.error("콘텐츠 수집 Job 실행 실패 - runDate={}", runDate, exception);
        }
    }
}
