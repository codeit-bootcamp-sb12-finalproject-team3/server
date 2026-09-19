package com.moduplaylist.batch.job.playlistembedding;

import com.moduplaylist.batch.job.playlistembedding.tasklet.PlaylistEmbeddingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PlaylistEmbeddingJobConfig {

    public static final String JOB_NAME = "playlistEmbeddingJob";

    @Bean
    public Job playlistEmbeddingJob(
            JobRepository jobRepository,
            Step playlistEmbeddingStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(playlistEmbeddingStep)
                .build();
    }

    @Bean
    public Step playlistEmbeddingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PlaylistEmbeddingTasklet playlistEmbeddingTasklet
    ) {
        return new StepBuilder("playlistEmbeddingStep", jobRepository)
                .tasklet(playlistEmbeddingTasklet, transactionManager)
                .build();
    }
}
