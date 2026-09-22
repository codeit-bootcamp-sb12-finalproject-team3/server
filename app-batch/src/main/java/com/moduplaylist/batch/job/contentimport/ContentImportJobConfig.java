package com.moduplaylist.batch.job.contentimport;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Configuration
public class ContentImportJobConfig {
    public static final String JOB_NAME = "contentImportJob";
    public static final String RUN_DATE_PARAMETER = "runDate";

    @Bean
    public Job contentImportJob(
        JobRepository jobRepository,
        Step tmdbMovieImportStep,
        Step tmdbTvImportStep,
        Step sportsDbEventSyncStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .validator(runDateValidator())
            .start(tmdbMovieImportStep)
            .next(tmdbTvImportStep)
            .next(sportsDbEventSyncStep)
            .build();
    }

    @Bean
    public Step tmdbMovieImportStep(JobRepository repository, PlatformTransactionManager transactionManager,
        TmdbContentImportService service) {
        return new StepBuilder("tmdbMovieImportStep", repository)
            .tasklet((contribution, context) -> {
                service.importMovies(runDate(context));
                return org.springframework.batch.repeat.RepeatStatus.FINISHED;
            }, transactionManager)
            .transactionAttribute(noStepTransaction())
            .build();
    }

    @Bean
    public Step tmdbTvImportStep(JobRepository repository, PlatformTransactionManager transactionManager,
        TmdbContentImportService service) {
        return new StepBuilder("tmdbTvImportStep", repository)
            .tasklet((contribution, context) -> {
                service.importTvSeasons(runDate(context));
                return org.springframework.batch.repeat.RepeatStatus.FINISHED;
            }, transactionManager)
            .transactionAttribute(noStepTransaction())
            .build();
    }

    @Bean
    public Step sportsDbEventSyncStep(JobRepository repository, PlatformTransactionManager transactionManager,
        SportsDbContentImportService service) {
        return new StepBuilder("sportsDbEventSyncStep", repository)
            .tasklet((contribution, context) -> {
                service.syncEvents(runDate(context));
                return org.springframework.batch.repeat.RepeatStatus.FINISHED;
            }, transactionManager)
            .transactionAttribute(noStepTransaction())
            .build();
    }

    private static DefaultTransactionAttribute noStepTransaction() {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        return attribute;
    }

    private static JobParametersValidator runDateValidator() {
        return parameters -> {
            String value = parameters.getString(RUN_DATE_PARAMETER);
            if (value == null || value.isBlank()) {
                throw new JobParametersInvalidException("runDate JobParameter는 필수입니다.");
            }
            try {
                LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException exception) {
                throw new JobParametersInvalidException(
                    "runDate JobParameter는 YYYY-MM-DD 형식이어야 합니다."
                );
            }
        };
    }

    private static LocalDate runDate(ChunkContext context) {
        String value = context.getStepContext()
            .getStepExecution()
            .getJobParameters()
            .getString(RUN_DATE_PARAMETER);
        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
