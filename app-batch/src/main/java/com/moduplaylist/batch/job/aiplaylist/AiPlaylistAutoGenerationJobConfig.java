package com.moduplaylist.batch.job.aiplaylist;

import com.moduplaylist.batch.job.aiplaylist.tasklet.AiPlaylistAutoGenerationTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AiPlaylistAutoGenerationJobConfig {

  public static final String JOB_NAME = "aiPlaylistAutoGenerationJob";

  @Bean
  public Job aiPlaylistAutoGenerationJob(
      JobRepository jobRepository,
      Step aiPlaylistAutoGenerationStep
  ) {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(aiPlaylistAutoGenerationStep)
        .build();
  }

  @Bean
  public Step aiPlaylistAutoGenerationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      AiPlaylistAutoGenerationTasklet aiPlaylistAutoGenerationTasklet
  ) {
    return new StepBuilder("aiPlaylistAutoGenerationStep", jobRepository)
        .tasklet(aiPlaylistAutoGenerationTasklet, transactionManager)
        .build();
  }
}
