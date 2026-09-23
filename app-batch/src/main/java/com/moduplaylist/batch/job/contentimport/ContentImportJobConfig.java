package com.moduplaylist.batch.job.contentimport;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Configuration
@Slf4j
public class ContentImportJobConfig {
    public static final String JOB_NAME = "contentImportJob";
    public static final String RUN_DATE_PARAMETER = "runDate";
    private static final Set<String> SOURCE_STEP_NAMES = Set.of(
        "tmdbMovieImportStep",
        "tmdbTvImportStep",
        "sportsDbEventSyncStep"
    );

    @Bean
    public Job contentImportJob(
        JobRepository jobRepository,
        Step tmdbMovieImportStep,
        Step tmdbTvImportStep,
        Step sportsDbEventSyncStep,
        JobExecutionDecider contentImportResultDecider
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .validator(runDateValidator())
            .start(tmdbMovieImportStep)
            .on("*").to(tmdbTvImportStep)
            .on("*").to(sportsDbEventSyncStep)
            .on("*").to(contentImportResultDecider)
            .on(FlowExecutionStatus.FAILED.getName()).fail()
            .from(contentImportResultDecider).on("*").end()
            .end()
            .build();
    }

    @Bean
    public JobExecutionDecider contentImportResultDecider() {
        return (jobExecution, stepExecution) -> jobExecution.getStepExecutions().stream()
            .filter(execution -> SOURCE_STEP_NAMES.contains(execution.getStepName()))
            .anyMatch(execution -> execution.getStatus() == BatchStatus.FAILED)
                ? FlowExecutionStatus.FAILED
                : FlowExecutionStatus.COMPLETED;
    }

    @Bean
    public Step tmdbMovieImportStep(JobRepository repository, PlatformTransactionManager transactionManager,
        TmdbContentImportService service) {
        return new StepBuilder("tmdbMovieImportStep", repository)
            .tasklet((contribution, context) -> executeImport(
                contribution,
                context,
                "TMDB_MOVIE",
                metrics -> service.importMovies(runDate(context), metrics)
            ), transactionManager)
            .transactionAttribute(noStepTransaction())
            .build();
    }

    @Bean
    public Step tmdbTvImportStep(JobRepository repository, PlatformTransactionManager transactionManager,
        TmdbContentImportService service) {
        return new StepBuilder("tmdbTvImportStep", repository)
            .tasklet((contribution, context) -> executeImport(
                contribution,
                context,
                "TMDB_TV",
                metrics -> service.importTvSeasons(runDate(context), metrics)
            ), transactionManager)
            .transactionAttribute(noStepTransaction())
            .build();
    }

    @Bean
    public Step sportsDbEventSyncStep(JobRepository repository, PlatformTransactionManager transactionManager,
        SportsDbContentImportService service) {
        return new StepBuilder("sportsDbEventSyncStep", repository)
            .tasklet((contribution, context) -> executeImport(
                contribution,
                context,
                "THESPORTSDB",
                metrics -> service.syncEvents(runDate(context), metrics)
            ), transactionManager)
            .transactionAttribute(noStepTransaction())
            .build();
    }

    private static RepeatStatus executeImport(
        StepContribution contribution,
        ChunkContext context,
        String source,
        Consumer<ContentImportMetrics> action
    ) {
        ContentImportMetrics metrics = new ContentImportMetrics();
        boolean completed = false;
        try {
            action.accept(metrics);
            if (metrics.hasFailures()) {
                throw new IllegalStateException(source + " 콘텐츠 수집에 부분 실패가 발생했습니다.");
            }
            completed = true;
            return RepeatStatus.FINISHED;
        } finally {
            metrics.writeTo(context.getStepContext().getStepExecution().getExecutionContext());
            ExitStatus status = completed ? ExitStatus.COMPLETED : ExitStatus.FAILED;
            contribution.setExitStatus(status.addExitDescription(metrics.summary()));
            log.info("콘텐츠 수집 Step 결과 - source={}, {}", source, metrics.summary());
        }
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
