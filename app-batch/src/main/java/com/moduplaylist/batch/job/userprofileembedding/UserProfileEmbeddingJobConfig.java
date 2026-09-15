package com.moduplaylist.batch.job.userprofileembedding;

import com.moduplaylist.batch.job.userprofileembedding.tasklet.UserProfileEmbeddingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class UserProfileEmbeddingJobConfig {

    public static final String JOB_NAME = "userProfileEmbeddingJob";

    @Bean
    public Job userProfileEmbeddingJob(
            JobRepository jobRepository,
            Step userProfileEmbeddingStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(userProfileEmbeddingStep)
                .build();
    }

    @Bean
    public Step userProfileEmbeddingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            UserProfileEmbeddingTasklet userProfileEmbeddingTasklet
    ) {
        return new StepBuilder("userProfileEmbeddingStep", jobRepository)
                .tasklet(userProfileEmbeddingTasklet, transactionManager)
                .build();
    }
}