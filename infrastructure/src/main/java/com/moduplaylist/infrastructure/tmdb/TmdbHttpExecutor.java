package com.moduplaylist.infrastructure.tmdb;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbHttpExecutor {

    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);
    private static final Duration DEFAULT_RATE_LIMIT_DELAY = Duration.ofSeconds(1);
    private static final Duration TRANSIENT_FAILURE_DELAY = Duration.ofMillis(500);

    private final HttpClient httpClient;

    public String execute(HttpRequest request, String operation) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                }
                if (attempt == 0 && RETRYABLE_STATUSES.contains(response.statusCode())) {
                    awaitRetry(response);
                    continue;
                }
                throw new TmdbWatchProviderException(
                    operation + "에 실패했습니다. status=" + response.statusCode()
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new TmdbWatchProviderException(operation + "가 중단되었습니다.", exception);
            } catch (IOException exception) {
                if (attempt == 0) {
                    await(TRANSIENT_FAILURE_DELAY);
                    continue;
                }
                throw new TmdbWatchProviderException(operation + "에 실패했습니다.", exception);
            }
        }
        throw new IllegalStateException("TMDB HTTP 재시도 처리가 종료되지 않았습니다.");
    }

    private static void awaitRetry(HttpResponse<?> response) {
        Duration delay = response.statusCode() == 429
            ? response.headers()
                .firstValue("Retry-After")
                .map(TmdbHttpExecutor::retryAfter)
                .orElse(DEFAULT_RATE_LIMIT_DELAY)
            : TRANSIENT_FAILURE_DELAY;
        await(delay);
    }

    private static Duration retryAfter(String value) {
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value.strip())));
        } catch (NumberFormatException ignored) {
            try {
                Instant retryAt = ZonedDateTime.parse(
                    value.strip(),
                    DateTimeFormatter.RFC_1123_DATE_TIME
                ).toInstant();
                return Duration.ofMillis(Math.max(
                    0,
                    Duration.between(Instant.now(), retryAt).toMillis()
                ));
            } catch (DateTimeParseException ignoredDate) {
                return DEFAULT_RATE_LIMIT_DELAY;
            }
        }
    }

    private static void await(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TmdbWatchProviderException("TMDB 재시도 대기가 중단되었습니다.", exception);
        }
    }
}
