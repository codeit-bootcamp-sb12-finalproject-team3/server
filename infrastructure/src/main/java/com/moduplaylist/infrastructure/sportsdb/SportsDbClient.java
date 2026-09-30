package com.moduplaylist.infrastructure.sportsdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException.FailureType;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SportsDbClient {
    private static final Duration TRANSIENT_FAILURE_DELAY = Duration.ofMillis(500);
    private static final Duration DEFAULT_RATE_LIMIT_DELAY = Duration.ofMinutes(1);
    private static final Duration MAX_RATE_LIMIT_DELAY = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SportsDbProperties properties;
    private long nextRequestAt;

    public JsonNode eventsOn(LocalDate date, String externalLeagueId) {
        return get("eventsday.php?d=" + encode(date.toString())
            + "&l=" + encode(externalLeagueId)).path("events");
    }

    public JsonNode event(int externalEventId) {
        return get("lookupevent.php?id=" + externalEventId)
            .path("events")
            .path(0);
    }

    private synchronized JsonNode get(String endpoint) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ExternalApiException(
                FailureType.UNAUTHORIZED,
                "TheSportsDB API key가 설정되지 않았습니다."
            );
        }
        String base = properties.getApiBaseUrl().replaceAll("/+$", "");
        URI uri = URI.create(base + "/" + properties.getApiKey() + "/" + endpoint);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .header("Accept", "application/json")
            .GET().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 429) {
            throw new SportsDbRateLimitException("TheSportsDB 요청 한도를 초과했습니다.");
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ExternalApiException(
                FailureType.UNAUTHORIZED,
                "TheSportsDB 인증에 실패했습니다. status=" + response.statusCode()
            );
        }
        if (response.statusCode() >= 500) {
            throw new ExternalApiException(
                FailureType.UNAVAILABLE,
                "TheSportsDB 서비스가 응답하지 않습니다. status=" + response.statusCode()
            );
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ExternalApiException(
                FailureType.ITEM_FAILURE,
                "TheSportsDB 조회에 실패했습니다. status=" + response.statusCode()
            );
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new ExternalApiException(
                FailureType.ITEM_FAILURE,
                "TheSportsDB 응답 처리에 실패했습니다.",
                exception
            );
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        for (int attempt = 0; attempt < 2; attempt++) {
            awaitRateLimit();
            try {
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                if (attempt == 0 && isRetryableStatus(response.statusCode())) {
                    if (response.statusCode() == 429) {
                        awaitRetryAfter(response);
                    } else {
                        await(TRANSIENT_FAILURE_DELAY);
                    }
                    continue;
                }
                return response;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ExternalApiException(
                    FailureType.UNAVAILABLE,
                    "TheSportsDB 조회가 중단되었습니다.",
                    exception
                );
            } catch (IOException exception) {
                if (attempt == 0) {
                    await(TRANSIENT_FAILURE_DELAY);
                    continue;
                }
                throw new ExternalApiException(
                    FailureType.UNAVAILABLE,
                    "TheSportsDB 조회에 실패했습니다.",
                    exception
                );
            }
        }
        throw new IllegalStateException("TheSportsDB HTTP 재시도 처리가 종료되지 않았습니다.");
    }

    private static boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private void awaitRetryAfter(HttpResponse<?> response) {
        Duration delay = response.headers()
            .firstValue("Retry-After")
            .map(SportsDbClient::retryAfter)
            .orElse(DEFAULT_RATE_LIMIT_DELAY);
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

    private static SportsDbRateLimitException rateLimitDelayExceeded(String value) {
        return new SportsDbRateLimitException(
            "TheSportsDB Retry-After가 최대 대기 시간 60초를 초과했습니다. value=" + value
        );
    }

    private void awaitRateLimit() {
        long waitMillis = nextRequestAt - System.currentTimeMillis();
        if (waitMillis > 0) {
            await(Duration.ofMillis(waitMillis));
        }
        nextRequestAt = System.currentTimeMillis() + properties.getRequestIntervalMillis();
    }

    private static void await(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException(
                FailureType.UNAVAILABLE,
                "TheSportsDB 요청 대기가 중단되었습니다.",
                exception
            );
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
