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
    prefix = "mopl.batch.ai-playlist.scheduler",
    name = "enabled",
    havingValue = "true"
)
public class AiPlaylistAutoGenerationJobScheduler {

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job aiPlaylistAutoGenerationJob;

  public AiPlaylistAutoGenerationJobScheduler(
      JobLauncher jobLauncher,
      JobExplorer jobExplorer,
      @Qualifier("aiPlaylistAutoGenerationJob") Job aiPlaylistAutoGenerationJob
  ) {
    this.jobLauncher = jobLauncher;
    this.jobExplorer = jobExplorer;
    this.aiPlaylistAutoGenerationJob = aiPlaylistAutoGenerationJob;
  }

  @Scheduled(
      cron = "${mopl.batch.ai-playlist.scheduler.cron}",
      zone = "${mopl.batch.ai-playlist.scheduler.zone}"
  )
  public void runAiPlaylistAutoGenerationJob() {
    if (!jobExplorer.findRunningJobExecutions(aiPlaylistAutoGenerationJob.getName()).isEmpty()) {
      log.warn("이미 실행 중인 AI 플레이리스트 자동 생성 배치 Job을 건너뜁니다.");
      return;
    }

    JobParameters parameters = new JobParametersBuilder()
        .addLong("requestedAt", System.currentTimeMillis())
        .toJobParameters();

    try {
      JobExecution execution = jobLauncher.run(aiPlaylistAutoGenerationJob, parameters);

      log.info(
          "AI 플레이리스트 자동 생성 배치 Job 실행 - executionId={}, status={}",
          execution.getId(),
          execution.getStatus()
      );
    } catch (Exception exception) {
      log.error("AI 플레이리스트 자동 생성 배치 Job 실행 실패", exception);
    }
  }
}
