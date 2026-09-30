package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentEmbeddingFields;
import com.moduplaylist.infrastructure.opensearch.content.ContentSearchDocumentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteIndexRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ContentEmbeddingCompletionService {

    private final ContentRepository contentRepository;
    private final ContentSearchDocumentRepository searchDocumentRepository;
    private final ContentIndexSynchronizer indexSynchronizer;
    private final ObjectProvider<ContentAutocompleteIndexRepository> autocompleteRepository;
    private final TransactionTemplate transactions;

    public boolean publishIfCurrent(
            UUID contentId,
            Instant sourceUpdatedAt,
            ContentEmbeddingFields embeddingFields
    ) {
        Boolean current = transactions.execute(status -> isCurrent(contentId, sourceUpdatedAt));
        if (!Boolean.TRUE.equals(current)) {
            return false;
        }

        if (autocompleteRepository.getIfAvailable() == null) {
            throw new IllegalStateException("Autocomplete index must be enabled for content embedding publication");
        }
        // Network calls run without a database transaction. A partial failure leaves pending=true.
        indexSynchronizer.synchronize(contentId);
        searchDocumentRepository.updateEmbedding(contentId, embeddingFields);

        Boolean completed = transactions.execute(status ->
                contentRepository.markEmbeddingCompleted(contentId, sourceUpdatedAt) == 1);
        return Boolean.TRUE.equals(completed);
    }

    private boolean isCurrent(UUID contentId, Instant sourceUpdatedAt) {
        UUID parentId = contentRepository.findParentId(contentId).orElse(null);
        Content parent = parentId == null ? null : contentRepository.findById(parentId).orElse(null);
        Content content = contentRepository.findById(contentId).orElse(null);
        return content != null
                && !content.isHidden()
                && (content.getType() == ContentType.MOVIE || content.getType() == ContentType.TV_SEASON)
                && content.isEmbeddingAllowedByAiTaggingStatus()
                && (content.getType() != ContentType.TV_SEASON
                    || (parent != null && !parent.isHidden()
                        && parent.getId().equals(content.getParentContent().getId())))
                && sourceUpdatedAt.equals(content.getEmbeddingSourceUpdatedAt());
    }
}
