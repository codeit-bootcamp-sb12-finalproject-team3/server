package com.moduplaylist.infrastructure.tmdb;

import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse.Provider;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse.Region;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TmdbKrWatchProviderExtractor {

    private static final String REGION_CODE = "KR";

    public Optional<Result> extract(TmdbWatchProviderResponse response) {
        Region region = response == null || response.results() == null
            ? null
            : response.results().get(REGION_CODE);
        if (region == null || region.link() == null || region.link().isBlank()) {
            return Optional.empty();
        }

        List<Provider> providers = Stream.of(
                region.flatrate(),
                region.free(),
                region.ads(),
                region.rent(),
                region.buy()
            )
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(
                Provider::providerId,
                Function.identity(),
                (existing, ignored) -> existing,
                LinkedHashMap::new
            ))
            .values()
            .stream()
            .toList();

        return Optional.of(new Result(region.link(), providers));
    }

    public record Result(String link, List<Provider> providers) {
    }
}
