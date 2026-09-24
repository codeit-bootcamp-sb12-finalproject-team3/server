package com.moduplaylist.infrastructure.tmdb;

import com.moduplaylist.infrastructure.externalapi.ExternalApiException;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException.FailureType;

public class TmdbWatchProviderException extends ExternalApiException {

    public TmdbWatchProviderException(String message) {
        super(FailureType.ITEM_FAILURE, message);
    }

    public TmdbWatchProviderException(String message, Throwable cause) {
        super(FailureType.ITEM_FAILURE, message, cause);
    }

    public TmdbWatchProviderException(FailureType failureType, String message) {
        super(failureType, message);
    }

    public TmdbWatchProviderException(
        FailureType failureType,
        String message,
        Throwable cause
    ) {
        super(failureType, message, cause);
    }
}
