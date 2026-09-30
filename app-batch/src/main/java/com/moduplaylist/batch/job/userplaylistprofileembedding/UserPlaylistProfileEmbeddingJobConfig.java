package com.moduplaylist.batch.job.userplaylistprofileembedding;

import com.moduplaylist.batch.job.userplaylistprofileembedding.tasklet.UserPlaylistProfileEmbeddingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class UserPlaylistProfileEmbeddingJobConfig {

    public static final String JOB_NAME = "userPlaylistProfileEmbeddingJob";

    @Bean
    public Job userPlaylistProfileEmbeddingJob(
            JobRepository jobRepository,
            Step userPlaylistProfileEmbeddingStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(userPlaylistProfileEmbeddingStep)
                .build();
    }

    @Bean
    public Step userPlaylistProfileEmbeddingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            UserPlaylistProfileEmbeddingTasklet tasklet
    ) {
        return new StepBuilder("userPlaylistProfileEmbeddingStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
