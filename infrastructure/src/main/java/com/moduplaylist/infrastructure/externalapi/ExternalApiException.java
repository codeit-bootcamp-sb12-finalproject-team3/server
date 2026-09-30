package com.moduplaylist.infrastructure.externalapi;

public class ExternalApiException extends RuntimeException {

    public enum FailureType {
        RATE_LIMITED,
        UNAUTHORIZED,
        UNAVAILABLE,
        ITEM_FAILURE
    }

    private final FailureType failureType;

    public ExternalApiException(FailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
    }

    public ExternalApiException(FailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public boolean isFatal() {
        return failureType != FailureType.ITEM_FAILURE;
    }
}
