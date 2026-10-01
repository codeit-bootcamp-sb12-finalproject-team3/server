package com.moduplaylist.batch.job.watchpartycleanup;

import com.moduplaylist.batch.job.watchpartycleanup.tasklet.WatchPartyCleanupTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class WatchPartyCleanupJobConfig {

    public static final String JOB_NAME = "watchPartyCleanupJob";

    @Bean
    public Job watchPartyCleanupJob(JobRepository jobRepository, Step watchPartyCleanupStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(watchPartyCleanupStep)
                .build();
    }

    @Bean
    public Step watchPartyCleanupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            WatchPartyCleanupTasklet watchPartyCleanupTasklet
    ) {
        return new StepBuilder("watchPartyCleanupStep", jobRepository)
                .tasklet(watchPartyCleanupTasklet, transactionManager)
                .build();
    }
}