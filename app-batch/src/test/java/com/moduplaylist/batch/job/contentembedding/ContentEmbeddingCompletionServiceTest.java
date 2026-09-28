package com.moduplaylist.batch.job.contentembedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentEmbeddingFields;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteIndexRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import com.moduplaylist.infrastructure.opensearch.content.ContentSearchDocumentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionTemplate;

class ContentEmbeddingCompletionServiceTest {

    private ContentRepository contentRepository;
    private ContentSearchDocumentRepository searchDocumentRepository;
    private ContentIndexSynchronizer indexSynchronizer;
    private ContentEmbeddingCompletionService service;

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        searchDocumentRepository = mock(ContentSearchDocumentRepository.class);
        indexSynchronizer = mock(ContentIndexSynchronizer.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ContentAutocompleteIndexRepository> autocompleteRepository =
                mock(ObjectProvider.class);
        when(autocompleteRepository.getIfAvailable())
                .thenReturn(mock(ContentAutocompleteIndexRepository.class));
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(invocation ->
                invocation.<org.springframework.transaction.support.TransactionCallback<?>>getArgument(0)
                        .doInTransaction(null));
        service = new ContentEmbeddingCompletionService(
                contentRepository, searchDocumentRepository, indexSynchronizer,
                autocompleteRepository, transactions);
    }

    @Test
    void publishesAndCompletesWhenSourceIsStillCurrent() {
        UUID contentId = UUID.randomUUID();
        Content content = movie();
        Instant sourceUpdatedAt = content.getEmbeddingSourceUpdatedAt();
        ContentEmbeddingFields fields = fields(sourceUpdatedAt);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.markEmbeddingCompleted(contentId, sourceUpdatedAt)).thenReturn(1);

        boolean published = service.publishIfCurrent(contentId, sourceUpdatedAt, fields);

        assertThat(published).isTrue();
        verify(indexSynchronizer).synchronize(contentId);
        verify(searchDocumentRepository).updateEmbedding(contentId, fields);
        verify(contentRepository).markEmbeddingCompleted(contentId, sourceUpdatedAt);
    }

    @Test
    void publishesReconciliationTargetEvenWhenPendingIsFalse() {
        UUID contentId = UUID.randomUUID();
        Instant sourceUpdatedAt = Instant.now();
        Content content = mock(Content.class);
        ContentEmbeddingFields fields = fields(sourceUpdatedAt);
        when(content.isHidden()).thenReturn(false);
        when(content.getType()).thenReturn(ContentType.MOVIE);
        when(content.isEmbeddingAllowedByAiTaggingStatus()).thenReturn(true);
        when(content.getEmbeddingSourceUpdatedAt()).thenReturn(sourceUpdatedAt);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.markEmbeddingCompleted(contentId, sourceUpdatedAt)).thenReturn(1);

        boolean published = service.publishIfCurrent(contentId, sourceUpdatedAt, fields);

        assertThat(content.isEmbeddingPending()).isFalse();
        assertThat(published).isTrue();
        verify(searchDocumentRepository).updateEmbedding(contentId, fields);
        verify(contentRepository).markEmbeddingCompleted(contentId, sourceUpdatedAt);
    }

    @Test
    void skipsPublishingWhenContentWasHidden() {
        UUID contentId = UUID.randomUUID();
        Content content = movie();
        Instant sourceUpdatedAt = content.getEmbeddingSourceUpdatedAt();
        content.hide();
        ContentEmbeddingFields fields = fields(sourceUpdatedAt);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        boolean published = service.publishIfCurrent(contentId, sourceUpdatedAt, fields);

        assertThat(published).isFalse();
        verify(searchDocumentRepository, never()).updateEmbedding(contentId, fields);
        verify(contentRepository, never()).markEmbeddingCompleted(contentId, sourceUpdatedAt);
    }

    @Test
    void skipsPublishingWhenSourceChangedDuringEmbedding() {
        UUID contentId = UUID.randomUUID();
        Content content = movie();
        Instant staleSourceUpdatedAt = content.getEmbeddingSourceUpdatedAt().minusSeconds(1);
        ContentEmbeddingFields fields = fields(staleSourceUpdatedAt);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        boolean published = service.publishIfCurrent(contentId, staleSourceUpdatedAt, fields);

        assertThat(published).isFalse();
        verify(searchDocumentRepository, never()).updateEmbedding(contentId, fields);
        verify(contentRepository, never()).markEmbeddingCompleted(contentId, staleSourceUpdatedAt);
    }

    @Test
    void skipsPublishingWhileTmdbTaggingIsPending() {
        UUID contentId = UUID.randomUUID();
        Content content = Content.builder()
                .title("pending movie")
                .type(ContentType.MOVIE)
                .externalSource("TMDB")
                .externalId(1)
                .build();
        content.updateAiTaggingStatus(Content.AiTaggingStatus.PENDING);
        Instant sourceUpdatedAt = content.getEmbeddingSourceUpdatedAt();
        ContentEmbeddingFields fields = fields(sourceUpdatedAt);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        boolean published = service.publishIfCurrent(contentId, sourceUpdatedAt, fields);

        assertThat(published).isFalse();
        verify(indexSynchronizer, never()).synchronize(contentId);
        verify(searchDocumentRepository, never()).updateEmbedding(contentId, fields);
        verify(contentRepository, never()).markEmbeddingCompleted(contentId, sourceUpdatedAt);
    }

    private Content movie() {
        return Content.builder()
                .title("test movie")
                .type(ContentType.MOVIE)
                .description("description")
                .build();
    }

    private ContentEmbeddingFields fields(Instant sourceUpdatedAt) {
        return ContentEmbeddingFields.builder()
                .embedding(new float[]{0.1f, 0.2f})
                .embeddingModel("test-model")
                .sourceUpdatedAt(sourceUpdatedAt)
                .embeddedAt(Instant.now())
                .build();
    }
}
