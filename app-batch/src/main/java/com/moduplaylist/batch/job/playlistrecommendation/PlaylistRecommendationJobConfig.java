package com.moduplaylist.batch.job.playlistrecommendation;

import com.moduplaylist.batch.job.playlistrecommendation.tasklet.PlaylistRecommendationTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PlaylistRecommendationJobConfig {

    public static final String JOB_NAME = "playlistRecommendationJob";

    @Bean
    public Job playlistRecommendationJob(
            JobRepository jobRepository,
            Step playlistRecommendationStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(playlistRecommendationStep)
                .build();
    }

    @Bean
    public Step playlistRecommendationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PlaylistRecommendationTasklet tasklet
    ) {
        return new StepBuilder("playlistRecommendationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
