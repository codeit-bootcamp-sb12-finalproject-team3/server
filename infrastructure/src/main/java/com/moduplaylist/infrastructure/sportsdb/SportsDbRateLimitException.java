package com.moduplaylist.infrastructure.sportsdb;

public class SportsDbRateLimitException extends IllegalStateException {

    public SportsDbRateLimitException(String message) {
        super(message);
    }
}
