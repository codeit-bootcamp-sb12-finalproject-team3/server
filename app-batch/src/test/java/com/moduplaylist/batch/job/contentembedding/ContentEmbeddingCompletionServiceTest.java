package com.moduplaylist.batch.job.contentembedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentEmbeddingCompletionServiceTest {

    private ContentRepository contentRepository;
    private ContentVectorRepository vectorRepository;
    private ContentEmbeddingCompletionService service;

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        vectorRepository = mock(ContentVectorRepository.class);
        service = new ContentEmbeddingCompletionService(contentRepository, vectorRepository);
    }

    @Test
    void publishesAndCompletesWhenSourceIsStillCurrent() {
        UUID contentId = UUID.randomUUID();
        Content content = movie();
        Instant sourceUpdatedAt = content.getEmbeddingSourceUpdatedAt();
        ContentVectorDocument document = document(contentId, sourceUpdatedAt);
        when(contentRepository.findByIdForUpdate(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.markEmbeddingCompleted(contentId, sourceUpdatedAt)).thenReturn(1);

        boolean published = service.publishIfCurrent(contentId, sourceUpdatedAt, document);

        assertThat(published).isTrue();
        verify(vectorRepository).upsert(document);
        verify(contentRepository).markEmbeddingCompleted(contentId, sourceUpdatedAt);
    }

    @Test
    void publishesReconciliationTargetEvenWhenPendingIsFalse() {
        UUID contentId = UUID.randomUUID();
        Instant sourceUpdatedAt = Instant.now();
        Content content = mock(Content.class);
        ContentVectorDocument document = document(contentId, sourceUpdatedAt);
        when(content.isHidden()).thenReturn(false);
        when(content.getEmbeddingSourceUpdatedAt()).thenReturn(sourceUpdatedAt);
        when(contentRepository.findByIdForUpdate(contentId)).thenReturn(Optional.of(content));
        when(contentRepository.markEmbeddingCompleted(contentId, sourceUpdatedAt)).thenReturn(1);

        boolean published = service.publishIfCurrent(contentId, sourceUpdatedAt, document);

        assertThat(content.isEmbeddingPending()).isFalse();
        assertThat(published).isTrue();
        verify(vectorRepository).upsert(document);
        verify(contentRepository).markEmbeddingCompleted(contentId, sourceUpdatedAt);
    }

    @Test
    void skipsPublishingWhenContentWasHidden() {
        UUID contentId = UUID.randomUUID();
        Content content = movie();
        Instant sourceUpdatedAt = content.getEmbeddingSourceUpdatedAt();
        content.hide();
        ContentVectorDocument document = document(contentId, sourceUpdatedAt);
        when(contentRepository.findByIdForUpdate(contentId)).thenReturn(Optional.of(content));

        boolean published = service.publishIfCurrent(contentId, sourceUpdatedAt, document);

        assertThat(published).isFalse();
        verify(vectorRepository, never()).upsert(document);
        verify(contentRepository, never()).markEmbeddingCompleted(contentId, sourceUpdatedAt);
    }

    @Test
    void skipsPublishingWhenSourceChangedDuringEmbedding() {
        UUID contentId = UUID.randomUUID();
        Content content = movie();
        Instant staleSourceUpdatedAt = content.getEmbeddingSourceUpdatedAt().minusSeconds(1);
        ContentVectorDocument document = document(contentId, staleSourceUpdatedAt);
        when(contentRepository.findByIdForUpdate(contentId)).thenReturn(Optional.of(content));

        boolean published = service.publishIfCurrent(contentId, staleSourceUpdatedAt, document);

        assertThat(published).isFalse();
        verify(vectorRepository, never()).upsert(document);
        verify(contentRepository, never()).markEmbeddingCompleted(contentId, staleSourceUpdatedAt);
    }

    private Content movie() {
        return Content.builder()
                .title("test movie")
                .type(ContentType.MOVIE)
                .description("description")
                .build();
    }

    private ContentVectorDocument document(UUID contentId, Instant sourceUpdatedAt) {
        return ContentVectorDocument.builder()
                .contentId(contentId)
                .type(ContentType.MOVIE.getValue())
                .title("test movie")
                .description("description")
                .sourceUpdatedAt(sourceUpdatedAt)
                .build();
    }
}
