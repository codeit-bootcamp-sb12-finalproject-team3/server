package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.batch.job.contentembedding.tasklet.ContentEmbeddingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ContentEmbeddingJobConfig {

    public static final String JOB_NAME = "contentEmbeddingJob";

    @Bean
    public Job contentEmbeddingJob(
            JobRepository jobRepository,
            Step contentEmbeddingStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(contentEmbeddingStep)
                .build();
    }

    @Bean
    public Step contentEmbeddingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ContentEmbeddingTasklet contentEmbeddingTasklet
    ) {
        return new StepBuilder("contentEmbeddingStep", jobRepository)
                .tasklet(contentEmbeddingTasklet, transactionManager)
                .build();
    }
}