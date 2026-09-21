package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentEmbeddingCompletionService {

    private final ContentRepository contentRepository;
    private final ContentVectorRepository vectorRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean publishIfCurrent(
            UUID contentId,
            Instant sourceUpdatedAt,
            ContentVectorDocument document
    ) {
        Content current = contentRepository.findByIdForUpdate(contentId).orElse(null);
        if (current == null
                || current.isHidden()
                || !sourceUpdatedAt.equals(current.getEmbeddingSourceUpdatedAt())) {
            return false;
        }

        vectorRepository.upsert(document);
        return contentRepository.markEmbeddingCompleted(contentId, sourceUpdatedAt) == 1;
    }
}
