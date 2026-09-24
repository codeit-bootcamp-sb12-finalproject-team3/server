package com.moduplaylist.infrastructure.sportsdb;

import com.moduplaylist.infrastructure.externalapi.ExternalApiException;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException.FailureType;

public class SportsDbRateLimitException extends ExternalApiException {

    public SportsDbRateLimitException(String message) {
        super(FailureType.RATE_LIMITED, message);
    }
}
