package com.moduplaylist.batch.job.contentrecommendation;

import com.moduplaylist.batch.job.contentrecommendation.tasklet.ContentRecommendationTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ContentRecommendationJobConfig {

    public static final String JOB_NAME = "contentRecommendationJob";

    @Bean
    public Job contentRecommendationJob(
            JobRepository jobRepository,
            Step contentRecommendationStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(contentRecommendationStep)
                .build();
    }

    @Bean
    public Step contentRecommendationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ContentRecommendationTasklet contentRecommendationTasklet
    ) {
        return new StepBuilder("contentRecommendationStep", jobRepository)
                .tasklet(contentRecommendationTasklet, transactionManager)
                .build();
    }
}
