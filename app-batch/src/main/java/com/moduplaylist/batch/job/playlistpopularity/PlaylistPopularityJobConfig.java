package com.moduplaylist.batch.job.playlistpopularity;

import com.moduplaylist.batch.job.playlistpopularity.tasklet.PlaylistPopularityTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PlaylistPopularityJobConfig {

  public static final String JOB_NAME = "playlistPopularityJob";

  @Bean
  public Job playlistPopularityJob(
      JobRepository jobRepository,
      Step playlistPopularityStep
  ) {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(playlistPopularityStep)
        .build();
  }

  @Bean
  public Step playlistPopularityStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      PlaylistPopularityTasklet playlistPopularityTasklet
  ) {
    return new StepBuilder("playlistPopularityStep", jobRepository)
        .tasklet(playlistPopularityTasklet, transactionManager)
        .build();
  }
}
