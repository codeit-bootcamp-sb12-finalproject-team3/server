package com.moduplaylist.infrastructure.tmdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbWatchProviderClient {

    private final TmdbHttpExecutor httpExecutor;
    private final ObjectMapper objectMapper;
    private final TmdbProperties properties;

    public TmdbWatchProviderResponse fetchMovie(int tmdbMovieId) {
        return fetch("movie/" + tmdbMovieId + "/watch/providers");
    }

    public TmdbWatchProviderResponse fetchTvSeason(int tmdbSeriesId, int seasonNumber) {
        return fetch("tv/" + tmdbSeriesId + "/season/" + seasonNumber + "/watch/providers");
    }

    private TmdbWatchProviderResponse fetch(String path) {
        String accessToken = properties.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new TmdbWatchProviderException("TMDB access token이 설정되지 않았습니다.");
        }

        URI uri = URI.create(trimTrailingSlash(properties.getApiBaseUrl())
            + "/" + path);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .build();

        String body = httpExecutor.execute(request, "TMDB Watch Providers 조회");
        try {
            return objectMapper.readValue(body, TmdbWatchProviderResponse.class);
        } catch (IOException exception) {
            throw new TmdbWatchProviderException(
                "TMDB Watch Providers 응답 처리에 실패했습니다.", exception
            );
        }
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
