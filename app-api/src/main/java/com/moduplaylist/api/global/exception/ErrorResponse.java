package com.moduplaylist.api.global.exception;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final Instant timestamp;
    private final String code;
    private final String message;
    private final Map<String, Object> details;
    private final int status;
}