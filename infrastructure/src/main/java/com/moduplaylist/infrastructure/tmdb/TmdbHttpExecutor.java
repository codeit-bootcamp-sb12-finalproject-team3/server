package com.moduplaylist.infrastructure.tmdb;

import com.moduplaylist.infrastructure.externalapi.ExternalApiException.FailureType;
import java.io.IOException;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbHttpExecutor {

    private static final Duration DEFAULT_RATE_LIMIT_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_RATE_LIMIT_DELAY = Duration.ofSeconds(60);
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
                if (attempt == 0 && isRetryableStatus(response.statusCode())) {
                    awaitRetry(response);
                    continue;
                }
                throw httpFailure(operation, response.statusCode());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new TmdbWatchProviderException(
                    FailureType.UNAVAILABLE,
                    operation + "가 중단되었습니다.",
                    exception
                );
            } catch (IOException exception) {
                if (attempt == 0) {
                    await(TRANSIENT_FAILURE_DELAY);
                    continue;
                }
                throw new TmdbWatchProviderException(
                    FailureType.UNAVAILABLE,
                    operation + "에 실패했습니다.",
                    exception
                );
            }
        }
        throw new IllegalStateException("TMDB HTTP 재시도 처리가 종료되지 않았습니다.");
    }

    private static TmdbWatchProviderException httpFailure(String operation, int status) {
        FailureType failureType;
        if (status == 429) {
            failureType = FailureType.RATE_LIMITED;
        } else if (status == 401 || status == 403) {
            failureType = FailureType.UNAUTHORIZED;
        } else if (status >= 500) {
            failureType = FailureType.UNAVAILABLE;
        } else {
            failureType = FailureType.ITEM_FAILURE;
        }
        return new TmdbWatchProviderException(
            failureType,
            operation + "에 실패했습니다. status=" + status
        );
    }

    private static boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
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

    static Duration retryAfter(String value) {
        String normalized = value.strip();
        try {
            BigInteger seconds = new BigInteger(normalized);
            if (seconds.signum() <= 0) return Duration.ZERO;
            if (seconds.compareTo(BigInteger.valueOf(MAX_RATE_LIMIT_DELAY.toSeconds())) > 0) {
                throw rateLimitDelayExceeded(value);
            }
            return Duration.ofSeconds(seconds.longValueExact());
        } catch (NumberFormatException ignored) {
            try {
                Instant retryAt = ZonedDateTime.parse(
                    normalized,
                    DateTimeFormatter.RFC_1123_DATE_TIME
                ).toInstant();
                Duration delay = Duration.between(Instant.now(), retryAt);
                if (delay.isNegative() || delay.isZero()) return Duration.ZERO;
                if (delay.compareTo(MAX_RATE_LIMIT_DELAY) > 0) {
                    throw rateLimitDelayExceeded(value);
                }
                return delay;
            } catch (DateTimeParseException ignoredDate) {
                return DEFAULT_RATE_LIMIT_DELAY;
            }
        }
    }

    private static TmdbWatchProviderException rateLimitDelayExceeded(String value) {
        return new TmdbWatchProviderException(
            FailureType.RATE_LIMITED,
            "TMDB Retry-After가 최대 대기 시간 60초를 초과했습니다. value=" + value
        );
    }

    private static void await(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TmdbWatchProviderException(
                FailureType.UNAVAILABLE,
                "TMDB 재시도 대기가 중단되었습니다.",
                exception
            );
        }
    }
}
