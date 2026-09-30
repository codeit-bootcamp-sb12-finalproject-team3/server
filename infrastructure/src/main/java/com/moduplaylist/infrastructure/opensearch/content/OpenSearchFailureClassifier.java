package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.opensearch.client.opensearch._types.OpenSearchException;

public final class OpenSearchFailureClassifier {

    private OpenSearchFailureClassifier() {
    }

    public static boolean shouldAbortStep(RuntimeException exception) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }

        boolean openSearchFailure = false;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = exception;
        while (current != null && visited.add(current)) {
            if (current instanceof IOException || current instanceof TimeoutException) {
                return true;
            }
            if (current instanceof OpenSearchException openSearchException) {
                openSearchFailure = true;
                if (isAbortStatus(openSearchException.status())) {
                    return true;
                }
            }
            if (current instanceof ContentSearchUnavailableException) {
                openSearchFailure = true;
            }
            current = current.getCause();
        }

        return !openSearchFailure;
    }

    private static boolean isAbortStatus(int status) {
        return status == 401
                || status == 403
                || status == 429
                || (status >= 500 && status < 600);
    }
}
