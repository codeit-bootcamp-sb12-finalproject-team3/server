package com.moduplaylist.infrastructure.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbWatchProviderResponse(
    Map<String, Region> results
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Region(
        String link,
        List<Provider> flatrate,
        List<Provider> free,
        List<Provider> ads,
        List<Provider> rent,
        List<Provider> buy
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Provider(
        @JsonProperty("provider_id") Integer providerId,
        @JsonProperty("provider_name") String providerName,
        @JsonProperty("logo_path") String logoPath
    ) {
    }
}
