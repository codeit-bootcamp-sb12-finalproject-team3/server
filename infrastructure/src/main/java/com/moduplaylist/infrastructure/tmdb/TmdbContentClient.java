package com.moduplaylist.infrastructure.tmdb;

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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbContentClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TmdbProperties properties;

    public JsonNode discoverMovies(LocalDate from, LocalDate to, int page) {
        Map<String, String> query = commonDiscoverQuery(from, to, page);
        query.put("region", "KR");
        query.put("release_date.gte", query.remove("air_date.gte"));
        query.put("release_date.lte", query.remove("air_date.lte"));
        query.put("with_release_type", "2|3|4|6");
        return get("/discover/movie", query);
    }

    public JsonNode discoverTv(LocalDate from, LocalDate to, int page) {
        Map<String, String> query = commonDiscoverQuery(from, to, page);
        query.put("timezone", "Asia/Seoul");
        return get("/discover/tv", query);
    }

    public JsonNode movieDetails(int id, String language) {
        return get("/movie/" + id,
            Map.of("language", language, "append_to_response", "credits,release_dates"));
    }

    public JsonNode tvDetails(int id, String language) {
        return get("/tv/" + id, Map.of("language", language, "append_to_response", "aggregate_credits"));
    }

    public JsonNode seasonDetails(int seriesId, int seasonNumber, String language) {
        return get("/tv/" + seriesId + "/season/" + seasonNumber,
            Map.of("language", language, "append_to_response", "aggregate_credits"));
    }

    public JsonNode seasonDetailsOriginal(int seriesId, int seasonNumber) {
        return get("/tv/" + seriesId + "/season/" + seasonNumber,
            Map.of("append_to_response", "aggregate_credits"));
    }

    private Map<String, String> commonDiscoverQuery(LocalDate from, LocalDate to, int page) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("language", "ko-KR");
        query.put("air_date.gte", from.toString());
        query.put("air_date.lte", to.toString());
        query.put("sort_by", "popularity.desc");
        query.put("include_adult", "false");
        query.put("page", Integer.toString(page));
        return query;
    }

    private JsonNode get(String path, Map<String, String> query) {
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            throw new TmdbWatchProviderException("TMDB access token이 설정되지 않았습니다.");
        }
        String queryString = query.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
            .reduce((left, right) -> left + "&" + right)
            .orElse("");
        URI uri = URI.create(trimTrailingSlash(properties.getApiBaseUrl()) + path + "?" + queryString);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .header("Authorization", "Bearer " + properties.getAccessToken())
            .header("Accept", "application/json")
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TmdbWatchProviderException("TMDB 콘텐츠 조회에 실패했습니다. status=" + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TmdbWatchProviderException("TMDB 콘텐츠 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new TmdbWatchProviderException("TMDB 콘텐츠 응답 처리에 실패했습니다.", exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
