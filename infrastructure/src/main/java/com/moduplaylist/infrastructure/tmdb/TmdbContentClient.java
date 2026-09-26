package com.moduplaylist.infrastructure.tmdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException.FailureType;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbContentClient {

    private final TmdbHttpExecutor httpExecutor;
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

    public JsonNode discoverTvByNetworks(LocalDate from, LocalDate to, int page) {
        Map<String, String> query = commonDiscoverQuery(from, to, page);
        addTvDiscoverFilters(query);
        query.put("with_networks", "829|342|97|156|866|885|5841|627|809");
        return get("/discover/tv", query);
    }

    public JsonNode discoverTvByWatchProviders(LocalDate from, LocalDate to, int page) {
        Map<String, String> query = commonDiscoverQuery(from, to, page);
        addTvDiscoverFilters(query);
        query.put("watch_region", "KR");
        query.put("with_watch_monetization_types", "flatrate|free|ads|rent|buy");
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

    private static void addTvDiscoverFilters(Map<String, String> query) {
        query.put("timezone", "Asia/Seoul");
        query.put("with_origin_country", "KR|US|GB|JP|CN");
        query.put("with_type", "0|2|3|4|5");
    }

    private JsonNode get(String path, Map<String, String> query) {
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            throw new TmdbWatchProviderException(
                FailureType.UNAUTHORIZED,
                "TMDB access token이 설정되지 않았습니다."
            );
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
        String body = httpExecutor.execute(request, "TMDB 콘텐츠 조회");
        try {
            return objectMapper.readTree(body);
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
