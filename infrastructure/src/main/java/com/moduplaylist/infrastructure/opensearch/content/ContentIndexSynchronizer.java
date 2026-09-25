package com.moduplaylist.infrastructure.opensearch.content;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentIndexSynchronizer {

    private static final int MAX_SYNC_ATTEMPTS = 3;

    private final ContentIndexSourceLoader sourceLoader;
    private final ContentAutocompleteSynchronizer autocompleteSynchronizer;
    private final ContentSearchDocumentSynchronizer searchDocumentSynchronizer;

    public ContentIndexSource load(UUID contentId) {
        return sourceLoader.load(contentId);
    }

    public void synchronize(UUID contentId) {
        ContentIndexSource source = load(contentId);
        RuntimeException failure = synchronizeAutocompleteWithRetry(source);
        RuntimeException searchFailure = synchronizeSearchWithRetry(source);
        if (failure != null) {
            if (searchFailure != null) {
                failure.addSuppressed(searchFailure);
            }
            throw failure;
        }
        if (searchFailure != null) {
            throw searchFailure;
        }
    }

    public void synchronizeAutocomplete(ContentIndexSource source) {
        autocompleteSynchronizer.synchronize(source);
    }

    public void synchronizeSearch(ContentIndexSource source) {
        searchDocumentSynchronizer.synchronize(source);
    }

    private RuntimeException synchronizeAutocompleteWithRetry(ContentIndexSource source) {
        RuntimeException failure = null;
        for (int attempt = 1; attempt <= MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                synchronizeAutocomplete(source);
                return null;
            } catch (RuntimeException exception) {
                failure = exception;
            }
        }
        return failure;
    }

    private RuntimeException synchronizeSearchWithRetry(ContentIndexSource source) {
        RuntimeException failure = null;
        for (int attempt = 1; attempt <= MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                synchronizeSearch(source);
                return null;
            } catch (RuntimeException exception) {
                failure = exception;
            }
        }
        return failure;
    }
}
