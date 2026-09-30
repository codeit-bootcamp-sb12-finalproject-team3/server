package com.moduplaylist.batch.job.contenttagging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.core.content.ai.ContentTagGenerator;
import com.moduplaylist.core.content.ai.ContentTagInput;
import com.moduplaylist.core.content.ai.ContentTaggingException;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.domain.Pageable;

class ContentTaggingTaskletTest {

    @Test
    void keepsProviderFailuresPendingAndOpensCircuitAfterThreeConsecutiveContents() throws Exception {
        ContentRepository contents = mock(ContentRepository.class);
        TmdbKeywordService keywords = mock(TmdbKeywordService.class);
        ContentTaggingStore store = mock(ContentTaggingStore.class);
        ContentTagGenerator generator = mock(ContentTagGenerator.class);
        ContentTagGuard guard = mock(ContentTagGuard.class);
        ContentIndexSynchronizer indexSynchronizer = mock(ContentIndexSynchronizer.class);
        ContentTaggingOpenAiCircuitBreaker circuitBreaker =
                mock(ContentTaggingOpenAiCircuitBreaker.class);
        ContentTaggingProperties properties = new ContentTaggingProperties();
        properties.setMaxItems(10);

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID());
        ContentTaggingStore.Snapshot snapshot = new ContentTaggingStore.Snapshot(
                new ContentTagInput("movie", "title", List.of(), "description",
                        List.of(), "MOVIE", List.of(), List.of()),
                "fingerprint");
        when(contents.findPendingTaggingIds(any(), eq(Content.AiTaggingStatus.PENDING),
                any(), any(Pageable.class))).thenReturn(ids);
        when(store.load(any())).thenReturn(snapshot);
        when(generator.generate(any(), anyBoolean())).thenThrow(
                new ContentTaggingException("AI_API_UNAVAILABLE", true, false));
        ContentTaggingTasklet tasklet = new ContentTaggingTasklet(contents, keywords, store,
                generator, guard, properties, indexSynchronizer, circuitBreaker);

        RepeatStatus result = tasklet.execute(null, null);

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(generator, times(6)).generate(any(), anyBoolean());
        verify(store, never()).complete(any(), any(), any());
        verify(indexSynchronizer, never()).synchronize(any());
        verify(circuitBreaker).openAfterConsecutiveFailures(false);
    }

    @Test
    void failedProbeAdvancesCircuitAndKeepsContentPending() throws Exception {
        ContentRepository contents = mock(ContentRepository.class);
        TmdbKeywordService keywords = mock(TmdbKeywordService.class);
        ContentTaggingStore store = mock(ContentTaggingStore.class);
        ContentTagGenerator generator = mock(ContentTagGenerator.class);
        ContentTagGuard guard = mock(ContentTagGuard.class);
        ContentIndexSynchronizer indexSynchronizer = mock(ContentIndexSynchronizer.class);
        ContentTaggingOpenAiCircuitBreaker circuitBreaker =
                mock(ContentTaggingOpenAiCircuitBreaker.class);
        ContentTaggingProperties properties = new ContentTaggingProperties();
        properties.setMaxItems(10);
        UUID id = UUID.randomUUID();
        ContentTaggingStore.Snapshot snapshot = new ContentTaggingStore.Snapshot(
                new ContentTagInput("movie", "title", List.of(), "description",
                        List.of(), "MOVIE", List.of(), List.of()),
                "fingerprint");
        when(contents.findPendingTaggingIds(any(), eq(Content.AiTaggingStatus.PENDING),
                any(), any(Pageable.class))).thenReturn(List.of(id));
        when(store.load(id)).thenReturn(snapshot);
        when(generator.generate(any(), anyBoolean())).thenThrow(
                new ContentTaggingException("AI_API_UNAVAILABLE", true, false));
        ChunkContext context = mock(ChunkContext.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(context.getStepContext().getStepExecution().getJobParameters()
                .getString("circuitProbeToken")).thenReturn("probe-token");
        ContentTaggingTasklet tasklet = new ContentTaggingTasklet(contents, keywords, store,
                generator, guard, properties, indexSynchronizer, circuitBreaker);

        RepeatStatus result = tasklet.execute(null, context);

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(generator, times(2)).generate(any(), anyBoolean());
        verify(store, never()).complete(any(), any(), any());
        verify(circuitBreaker).probeFailed("probe-token", false);
        verify(circuitBreaker, never()).probeAborted(any());
    }

    @Test
    void permanentAuthenticationFailureOpensLongCircuitImmediately() throws Exception {
        ContentRepository contents = mock(ContentRepository.class);
        TmdbKeywordService keywords = mock(TmdbKeywordService.class);
        ContentTaggingStore store = mock(ContentTaggingStore.class);
        ContentTagGenerator generator = mock(ContentTagGenerator.class);
        ContentTagGuard guard = mock(ContentTagGuard.class);
        ContentIndexSynchronizer indexSynchronizer = mock(ContentIndexSynchronizer.class);
        ContentTaggingOpenAiCircuitBreaker circuitBreaker =
                mock(ContentTaggingOpenAiCircuitBreaker.class);
        ContentTaggingProperties properties = new ContentTaggingProperties();
        properties.setMaxItems(10);
        UUID id = UUID.randomUUID();
        ContentTaggingStore.Snapshot snapshot = new ContentTaggingStore.Snapshot(
                new ContentTagInput("movie", "title", List.of(), "description",
                        List.of(), "MOVIE", List.of(), List.of()),
                "fingerprint");
        when(contents.findPendingTaggingIds(any(), eq(Content.AiTaggingStatus.PENDING),
                any(), any(Pageable.class))).thenReturn(List.of(id));
        when(store.load(id)).thenReturn(snapshot);
        when(generator.generate(any(), anyBoolean())).thenThrow(
                new ContentTaggingException("AI_API_STATUS_401", false, true));
        ContentTaggingTasklet tasklet = new ContentTaggingTasklet(contents, keywords, store,
                generator, guard, properties, indexSynchronizer, circuitBreaker);

        RepeatStatus result = tasklet.execute(null, null);

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(generator).generate(any(), anyBoolean());
        verify(circuitBreaker).openAfterConsecutiveFailures(true);
        verify(store, never()).complete(any(), any(), any());
    }
}
