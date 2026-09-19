package com.moduplaylist.infrastructure.tmdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbWatchProviderClient {

    private final HttpClient tmdbHttpClient;
    private final ObjectMapper objectMapper;
    private final TmdbProperties properties;

    public TmdbWatchProviderResponse fetchMovie(int tmdbMovieId) {
        return fetch("movie", tmdbMovieId);
    }

    public TmdbWatchProviderResponse fetchTvSeries(int tmdbSeriesId) {
        return fetch("tv", tmdbSeriesId);
    }

    private TmdbWatchProviderResponse fetch(String contentType, int tmdbId) {
        String accessToken = properties.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new TmdbWatchProviderException("TMDB access token이 설정되지 않았습니다.");
        }

        URI uri = URI.create(trimTrailingSlash(properties.getApiBaseUrl())
            + "/" + contentType + "/" + tmdbId + "/watch/providers");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .build();

        try {
            HttpResponse<String> response = tmdbHttpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TmdbWatchProviderException(
                    "TMDB Watch Providers 조회에 실패했습니다. status=" + response.statusCode()
                );
            }
            return objectMapper.readValue(response.body(), TmdbWatchProviderResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TmdbWatchProviderException("TMDB Watch Providers 조회가 중단되었습니다.", e);
        } catch (IOException e) {
            throw new TmdbWatchProviderException("TMDB Watch Providers 응답 처리에 실패했습니다.", e);
        }
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
