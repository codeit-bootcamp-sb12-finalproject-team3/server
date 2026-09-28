package com.moduplaylist.batch.job.contenttagging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentTagGenerator;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.ai.content.SpringAiContentTagGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Configuration
@ConditionalOnProperty(prefix = "mopl.batch.content-tagging", name = "enabled", havingValue = "true")
public class ContentTaggingJobConfig {
    public static final String JOB_NAME = "contentAiTaggingJob";

    @Bean
    public ContentTagGenerator contentTagGenerator(ObjectMapper mapper, ContentTaggingProperties properties,
        @Value("${spring.ai.openai.api-key:}") String key,
        @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
        @Value("${mopl.opensearch.enabled:false}") boolean searchEnabled) {
        if (key.isBlank() || properties.getModel().isBlank() || properties.getTimeoutSeconds() <= 0
            || properties.getMaxTokens() <= 0 || properties.getMaxItems() <= 0) {
            throw new IllegalStateException("Invalid content tagging configuration");
        }
        if (!searchEnabled) throw new IllegalStateException("Content tagging requires OpenSearch publication");
        return new SpringAiContentTagGenerator(mapper, key, baseUrl, properties.getModel(),
            properties.getTimeoutSeconds(), properties.getMaxTokens());
    }

    @Bean
    public ContentTaggingTasklet contentTaggingTasklet(ContentRepository contents, TmdbKeywordService keywords,
        ContentTaggingStore store, ContentTagGenerator generator, ContentTagGuard guard,
        ContentTaggingProperties properties, ContentIndexSynchronizer indexSynchronizer) {
        return new ContentTaggingTasklet(contents, keywords, store, generator, guard, properties,
            indexSynchronizer);
    }

    @Bean
    public Step contentAiTaggingStep(JobRepository repository, PlatformTransactionManager transactions,
                                    ContentTaggingTasklet tasklet) {
        var noTransaction = new DefaultTransactionAttribute();
        noTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        return new StepBuilder("contentAiTaggingStep", repository).tasklet(tasklet, transactions)
            .transactionAttribute(noTransaction).build();
    }

    @Bean
    public Job contentAiTaggingJob(JobRepository repository, Step contentAiTaggingStep) {
        return new JobBuilder(JOB_NAME, repository).start(contentAiTaggingStep).build();
    }
}
