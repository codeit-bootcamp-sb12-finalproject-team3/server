package com.moduplaylist.infrastructure.tmdb;

public class TmdbWatchProviderException extends RuntimeException {

    public TmdbWatchProviderException(String message) {
        super(message);
    }

    public TmdbWatchProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
