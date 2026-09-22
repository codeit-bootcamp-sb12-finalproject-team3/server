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
    prefix = "mopl.batch.playlist-popularity.scheduler",
    name = "enabled",
    havingValue = "true"
)
public class PlaylistPopularityJobScheduler {
  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job playlistPopularityJob;

  public PlaylistPopularityJobScheduler(
      JobLauncher jobLauncher,
      JobExplorer jobExplorer,
      @Qualifier("playlistPopularityJob") Job playlistPopularityJob
  ) {
    this.jobLauncher = jobLauncher;
    this.jobExplorer = jobExplorer;
    this.playlistPopularityJob = playlistPopularityJob;
  }

  @Scheduled(
      cron = "${mopl.batch.playlist-popularity.scheduler.cron}",
      zone = "${mopl.batch.playlist-popularity.scheduler.zone}"
  )
  public void runPlaylistPopularityJob() {
    if (!jobExplorer.findRunningJobExecutions(playlistPopularityJob.getName()).isEmpty()) {
      log.warn("이미 실행 중인 주간 인기 플레이리스트 배치 Job을 건너뜁니다.");
      return;
    }

    JobParameters parameters =
        new JobParametersBuilder()
            .addLong("requestedAt", System.currentTimeMillis())
            .toJobParameters();

    try {
      JobExecution execution = jobLauncher.run(playlistPopularityJob, parameters);

      log.info(
          "주간 인기 플레이리스트 배치 Job 실행 - executionId={}, status={}",
          execution.getId(),
          execution.getStatus()
      );
    } catch (Exception exception) {
      log.error("주간 인기 플레이리스트 배치 Job 실행 실패", exception);
    }
  }
}
