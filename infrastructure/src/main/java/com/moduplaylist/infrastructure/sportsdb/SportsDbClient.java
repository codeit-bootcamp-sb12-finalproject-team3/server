package com.moduplaylist.infrastructure.sportsdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
        String base = properties.getApiBaseUrl().replaceAll("/+$", "");
        URI uri = URI.create(base + "/" + properties.getApiKey() + "/" + endpoint);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .header("Accept", "application/json")
            .GET().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 429) {
            awaitRetryAfter(response);
            response = send(request);
        }
        if (response.statusCode() == 429) {
            throw new SportsDbRateLimitException("TheSportsDB 요청 한도를 초과했습니다.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("TheSportsDB 조회에 실패했습니다. status=" + response.statusCode());
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("TheSportsDB 응답 처리에 실패했습니다.", exception);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        awaitRateLimit();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TheSportsDB 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("TheSportsDB 조회에 실패했습니다.", exception);
        }
    }

    private void awaitRetryAfter(HttpResponse<?> response) {
        long waitMillis = response.headers()
            .firstValue("Retry-After")
            .map(SportsDbClient::retryAfterMillis)
            .orElse(Duration.ofMinutes(1).toMillis());
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TheSportsDB 재시도 대기가 중단되었습니다.", exception);
        }
    }

    private static long retryAfterMillis(String value) {
        try {
            return Math.max(0, Duration.ofSeconds(Long.parseLong(value.strip())).toMillis());
        } catch (NumberFormatException ignored) {
            try {
                Instant retryAt = ZonedDateTime.parse(
                    value.strip(),
                    DateTimeFormatter.RFC_1123_DATE_TIME
                ).toInstant();
                return Math.max(0, Duration.between(Instant.now(), retryAt).toMillis());
            } catch (DateTimeParseException ignoredDate) {
                return Duration.ofMinutes(1).toMillis();
            }
        }
    }

    private void awaitRateLimit() {
        long waitMillis = nextRequestAt - System.currentTimeMillis();
        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("TheSportsDB 요청 대기가 중단되었습니다.", exception);
            }
        }
        nextRequestAt = System.currentTimeMillis() + properties.getRequestIntervalMillis();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
