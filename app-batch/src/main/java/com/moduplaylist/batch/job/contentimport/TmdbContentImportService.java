package com.moduplaylist.batch.job.contentimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.Content.AiTaggingStatus;
import com.moduplaylist.core.content.entity.ContentCast;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.Episode;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.repository.ContentCastRepository;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.EpisodeRepository;
import com.moduplaylist.core.content.repository.GenreRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteSynchronizer;
import com.moduplaylist.infrastructure.tmdb.TmdbContentClient;
import com.moduplaylist.infrastructure.tmdb.TmdbProperties;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderClient;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbContentImportService {
    private static final String SOURCE = "TMDB";
    private static final int CAST_LIMIT = 10;

    private final TmdbContentClient tmdbClient;
    private final TmdbWatchProviderClient watchProviderClient;
    private final TmdbContentPlatformService contentPlatformService;
    private final TmdbProperties properties;
    private final ContentRepository contentRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;
    private final ContentGenreRepository contentGenreRepository;
    private final ContentCastRepository contentCastRepository;
    private final ContentAutocompleteSynchronizer autocompleteSynchronizer;
    private final TransactionTemplate transactionTemplate;

    public void importMovies(LocalDate runDate, ContentImportMetrics metrics) {
        LocalDate from = runDate.minusDays(1);
        Set<Integer> ids = new HashSet<>();
        int totalPages = 1;
        for (int page = 1; page <= totalPages; page++) {
            JsonNode response = tmdbClient.discoverMovies(from, runDate, page);
            response.path("results").forEach(candidate -> {
                int id = candidate.path("id").asInt();
                if (id > 0 && ids.add(id)) metrics.candidate();
            });
            totalPages = response.path("total_pages").asInt(1);
        }
        ids.forEach(id -> fetchAndImportMovieSafely(id, metrics));
    }

    public void importTvSeasons(LocalDate runDate, ContentImportMetrics metrics) {
        LocalDate from = runDate.minusDays(1);
        Set<Integer> ids = new HashSet<>();
        collectTvCandidates(ids, metrics,
            page -> tmdbClient.discoverTvByNetworks(from, runDate, page));
        collectTvCandidates(ids, metrics,
            page -> tmdbClient.discoverTvByWatchProviders(from, runDate, page));
        ids.forEach(id -> importSeriesSeasonsSafely(id, runDate, metrics));
    }

    private void collectTvCandidates(
        Set<Integer> ids,
        ContentImportMetrics metrics,
        IntFunction<JsonNode> requestPage
    ) {
        int totalPages = 1;
        for (int page = 1; page <= totalPages; page++) {
            JsonNode response = requestPage.apply(page);
            response.path("results").forEach(candidate -> {
                int id = candidate.path("id").asInt();
                if (id > 0 && ids.add(id)) metrics.candidate();
            });
            totalPages = response.path("total_pages").asInt(1);
        }
    }

    private void fetchAndImportMovieSafely(int id, ContentImportMetrics metrics) {
        try {
            fetchAndImportMovie(id, metrics);
        } catch (RuntimeException exception) {
            rethrowIfInterrupted(exception);
            metrics.failed("movie:" + id);
            log.warn("TMDB 영화 수집에 실패해 다음 콘텐츠를 처리합니다. tmdbId={}", id, exception);
        }
    }

    private void importSeriesSeasonsSafely(
        int seriesId,
        LocalDate runDate,
        ContentImportMetrics metrics
    ) {
        try {
            importSeriesSeasons(seriesId, runDate, metrics);
        } catch (RuntimeException exception) {
            rethrowIfInterrupted(exception);
            metrics.failed("tv-series:" + seriesId);
            log.warn("TMDB TV 시리즈 조회에 실패해 다음 콘텐츠를 처리합니다. tmdbId={}",
                seriesId, exception);
        }
    }

    private void fetchAndImportMovie(int id, ContentImportMetrics metrics) {
        Content existing = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.MOVIE, id).orElse(null);
        if (existing != null) {
            metrics.existing();
            if (!existing.isHidden()) {
                synchronizeMovieProvidersSafely(existing.getId(), id, metrics);
            }
            synchronizeAutocompleteSafely(existing.getId(), "movie:" + id, metrics);
            return;
        }
        JsonNode ko = tmdbClient.movieDetails(id, "ko-KR");
        JsonNode en = needsFallback(ko, "title", "overview", "poster_path")
            || !hasValidGenres(ko.path("genres"))
            ? tmdbClient.movieDetails(id, "en-US") : ko;
        String title = limit(firstText(ko, en, "title"), 255);
        String description = firstText(ko, en, "overview");
        JsonNode genres = hasValidGenres(ko.path("genres"))
            ? ko.path("genres") : en.path("genres");
        if (title == null || description == null || !hasValidGenres(genres)) {
            metrics.missingRequired();
            log.info("TMDB 영화 필수 정보가 없어 건너뜁니다. tmdbId={}", id);
            return;
        }
        Content movie = transactionTemplate.execute(status ->
            importMovie(id, ko, en, genres));
        if (movie == null) {
            metrics.existing();
            contentRepository.findByExternalSourceAndTypeAndExternalId(
                    SOURCE, ContentType.MOVIE, id)
                .ifPresent(content -> {
                    if (!content.isHidden()) {
                        synchronizeMovieProvidersSafely(content.getId(), id, metrics);
                    }
                    synchronizeAutocompleteSafely(content.getId(), "movie:" + id, metrics);
                });
            return;
        }
        metrics.created();
        synchronizeMovieProvidersSafely(movie.getId(), id, metrics);
        synchronizeAutocompleteSafely(movie.getId(), "movie:" + id, metrics);
    }

    private Content importMovie(
        int id,
        JsonNode ko,
        JsonNode en,
        JsonNode genres
    ) {
        if (contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.MOVIE, id).isPresent()) return null;
        String title = limit(firstText(ko, en, "title"), 255);
        String description = firstText(ko, en, "overview");
        Content movie = Content.builder()
            .title(title).type(ContentType.MOVIE).description(description)
            .thumbnailUrl(properties.imageUrl(firstText(ko, en, "poster_path")))
            .releaseDate(koreanMovieReleaseDate(ko, en))
            .runtime(positiveInt(ko.path("runtime")))
            .metadata(Map.of("originalTitle", text(ko, "original_title", title)))
            .externalSource(SOURCE).externalId(id).build();
        movie.updateAiTaggingStatus(AiTaggingStatus.PENDING);
        contentRepository.save(movie);
        saveGenres(movie, genres);
        saveCast(movie, ko.path("credits").path("cast"));
        return movie;
    }

    private void importSeriesSeasons(
        int seriesId,
        LocalDate runDate,
        ContentImportMetrics metrics
    ) {
        Content series = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SERIES, seriesId).orElse(null);
        if (series != null) metrics.existing();
        boolean sourceSeriesHidden = series != null && series.isHidden();
        JsonNode ko = tmdbClient.tvDetails(seriesId, "ko-KR");

        List<JsonNode> remoteSeasons = iterable(ko.path("seasons")).stream()
            .filter(candidate -> candidate.hasNonNull("id") && candidate.hasNonNull("season_number"))
            .filter(candidate -> candidate.path("season_number").asInt() >= 0)
            .toList();
        if (remoteSeasons.isEmpty()) return;

        Map<Integer, Content> existingSeasons = findExistingSeasonsByExternalId(remoteSeasons);
        boolean hasMissingRemoteSeason = remoteSeasons.stream().anyMatch(candidate ->
            !existingSeasons.containsKey(candidate.path("id").asInt()));
        Set<Integer> existingSeasonNumbers = series != null
            && !sourceSeriesHidden
            && hasMissingRemoteSeason
                ? new HashSet<>(
                    contentRepository.findAllSeasonNumbersByParentContentId(series.getId()))
                : Set.of();
        synchronizeExistingSeasons(existingSeasons.values(), metrics);
        Set<Integer> episodeSeasonNumbers = episodeSeasonNumbers(ko, runDate);
        Set<Integer> seasonNumbersToRefresh = refreshSeasonNumbers(
            episodeSeasonNumbers, remoteSeasons, runDate);

        List<JsonNode> candidates = remoteSeasons.stream()
            .filter(candidate -> shouldLoadSeasonDetails(
                candidate,
                existingSeasons.get(candidate.path("id").asInt()),
                sourceSeriesHidden,
                existingSeasonNumbers,
                episodeSeasonNumbers,
                seasonNumbersToRefresh,
                runDate
            ))
            .toList();
        if (candidates.isEmpty()) return;

        JsonNode en = needsFallback(ko, "name") || !hasValidGenres(ko.path("genres"))
            ? tmdbClient.tvDetails(seriesId, "en-US") : ko;
        if (series == null && firstText(ko, en, "name") == null
            && text(ko, "original_name", null) == null) {
            metrics.missingRequired();
            return;
        }
        JsonNode genres = hasValidGenres(ko.path("genres"))
            ? ko.path("genres") : en.path("genres");
        for (JsonNode candidate : candidates) {
            int number = candidate.path("season_number").asInt();
            String failureId = "tv-season:" + seriesId + "/" + number;
            try {
                JsonNode seasonKo = tmdbClient.seasonDetails(seriesId, number, "ko-KR");
                JsonNode seasonEn = needsSeasonEnglishFallback(seasonKo)
                    ? tmdbClient.seasonDetails(seriesId, number, "en-US")
                    : seasonKo;
                Content existing = existingSeasons.get(candidate.path("id").asInt());
                SeasonData data = new SeasonData(
                    candidate,
                    seasonKo,
                    seasonEn,
                    existing != null,
                    existing != null && existing.isHidden()
                );
                if (data.hidden()) continue;
                if (!data.existing() && missingSeasonRequired(data, genres)) {
                    metrics.missingRequired();
                    continue;
                }
                SeasonSaveResult result = transactionTemplate.execute(status ->
                    saveSeason(seriesId, data, ko, en, genres, runDate)
                );
                if (result != null && result.season() != null) {
                    metrics.created(result.createdCount());
                    synchronizeSeasonProvidersSafely(
                        result.season().getId(), seriesId, number, failureId, metrics);
                    synchronizeAutocompleteSafely(result.season().getId(), failureId, metrics);
                }
            } catch (RuntimeException exception) {
                rethrowIfInterrupted(exception);
                metrics.failed(failureId);
                log.warn("TMDB TV 시즌 수집에 실패해 다음 시즌을 처리합니다. seriesId={}, seasonNumber={}",
                    seriesId, number, exception);
            }
        }
    }

    private Map<Integer, Content> findExistingSeasonsByExternalId(List<JsonNode> remoteSeasons) {
        Set<Integer> externalIds = new HashSet<>();
        remoteSeasons.forEach(season -> externalIds.add(season.path("id").asInt()));
        if (externalIds.isEmpty()) return Map.of();

        Map<Integer, Content> existingSeasons = new LinkedHashMap<>();
        contentRepository.findAllByExternalSourceAndTypeAndExternalIdIn(
                SOURCE, ContentType.TV_SEASON, externalIds)
            .forEach(season -> existingSeasons.put(season.getExternalId(), season));
        return existingSeasons;
    }

    private void synchronizeExistingSeasons(
        Collection<Content> seasons,
        ContentImportMetrics metrics
    ) {
        for (Content season : seasons) {
            if (season.isHidden()) continue;
            metrics.existing();
        }
    }

    private static boolean missingSeasonRequired(SeasonData data, JsonNode genres) {
        return firstText(data.ko(), data.en(), "name") == null
            || firstText(data.ko(), data.en(), "overview") == null
            || !hasValidGenres(genres);
    }

    private static boolean needsSeasonEnglishFallback(JsonNode season) {
        return text(season, "name", null) == null
            || text(season, "overview", null) == null
            || iterable(season.path("episodes")).stream()
                .anyMatch(episode -> text(episode, "name", null) == null);
    }

    private SeasonSaveResult saveSeason(
        int seriesId,
        SeasonData data,
        JsonNode seriesKo,
        JsonNode seriesEn,
        JsonNode genres,
        LocalDate runDate
    ) {
        Content series = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SERIES, seriesId).orElse(null);
        int seasonId = data.candidate().path("id").asInt();
        Content existing = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SEASON, seasonId).orElse(null);
        if (existing != null) {
            if (existing.isHidden()) return SeasonSaveResult.empty();
            saveEpisodes(existing, data.ko(), data.en(), runDate);
            updateEpisodeCount(existing, remoteEpisodeCount(data));
            return new SeasonSaveResult(existing, 0);
        }
        if (series != null && series.isHidden()) return SeasonSaveResult.empty();
        if (series != null && contentRepository.findByParentContent_IdAndSeasonNumber(
            series.getId(), data.candidate().path("season_number").asInt()).isPresent()) {
            return SeasonSaveResult.empty();
        }
        boolean seriesCreated = false;
        if (series == null) {
            String seriesTitle = limit(firstText(seriesKo, seriesEn, "name"), 255);
            if (seriesTitle == null) {
                seriesTitle = limit(text(seriesKo, "original_name", null), 255);
            }
            if (seriesTitle == null) return SeasonSaveResult.empty();
            series = contentRepository.save(Content.builder()
                .title(seriesTitle).type(ContentType.TV_SERIES)
                .metadata(Map.of("originalTitle", text(seriesKo, "original_name", seriesTitle)))
                .externalSource(SOURCE).externalId(seriesId).build());
            seriesCreated = true;
        }
        Content season = importSeason(series, data, seriesKo, runDate);
        if (season == null) return SeasonSaveResult.empty();
        saveGenres(season, genres);
        saveCast(season, seasonCast(data, seriesKo, seriesEn));
        return new SeasonSaveResult(season, seriesCreated ? 2 : 1);
    }

    private Content importSeason(
        Content series,
        SeasonData data,
        JsonNode seriesKo,
        LocalDate runDate
    ) {
        JsonNode candidate = data.candidate();
        int number = candidate.path("season_number").asInt();
        JsonNode ko = data.ko();
        JsonNode en = data.en();
        String title = limit(firstText(ko, en, "name"), 255);
        String description = firstText(ko, en, "overview");
        if (title == null) return null;
        int seasonId = candidate.path("id").asInt();
        Content season = Content.builder()
            .parentContent(series).title(title).seasonNumber(number)
            .episodeCount(remoteEpisodeCount(data)).type(ContentType.TV_SEASON)
            .description(description).thumbnailUrl(properties.imageUrl(firstText(ko, en, "poster_path")))
            .releaseDate(date(firstText(ko, en, "air_date")))
            .metadata(Map.of("originalTitle", text(seriesKo, "original_name", title)))
            .externalSource(SOURCE).externalId(seasonId).build();
        season.updateAiTaggingStatus(AiTaggingStatus.PENDING);
        contentRepository.save(season);
        saveEpisodes(season, ko, en, runDate);
        return season;
    }

    private JsonNode seasonCast(SeasonData data, JsonNode seriesKo, JsonNode seriesEn) {
        JsonNode cast = data.ko().path("aggregate_credits").path("cast");
        if (!cast.isArray() || cast.isEmpty()) cast = seriesKo.path("aggregate_credits").path("cast");
        if ((!cast.isArray() || cast.isEmpty()) && seriesEn != seriesKo) {
            cast = seriesEn.path("aggregate_credits").path("cast");
        }
        return cast;
    }

    private void synchronizeMovieProvidersSafely(
        UUID contentId,
        int movieId,
        ContentImportMetrics metrics
    ) {
        try {
            TmdbWatchProviderResponse providers = watchProviderClient.fetchMovie(movieId);
            contentPlatformService.synchronizeProviders(contentId, providers);
        } catch (RuntimeException exception) {
            rethrowIfInterrupted(exception);
            metrics.failed("movie:" + movieId);
            log.warn("TMDB 영화 OTT 제공처 동기화에 실패했습니다. tmdbId={}",
                movieId, exception);
        }
    }

    private void synchronizeSeasonProvidersSafely(
        UUID contentId,
        int seriesId,
        int seasonNumber,
        String failureId,
        ContentImportMetrics metrics
    ) {
        try {
            TmdbWatchProviderResponse providers = watchProviderClient.fetchTvSeason(
                seriesId, seasonNumber);
            contentPlatformService.synchronizeProviders(contentId, providers);
        } catch (RuntimeException exception) {
            rethrowIfInterrupted(exception);
            metrics.failed(failureId);
            log.warn(
                "TMDB 시즌 OTT 제공처 동기화에 실패했습니다. seriesId={}, seasonNumber={}",
                seriesId, seasonNumber, exception
            );
        }
    }

    private void synchronizeAutocompleteSafely(
        UUID contentId,
        String failureId,
        ContentImportMetrics metrics
    ) {
        try {
            autocompleteSynchronizer.synchronize(contentId);
        } catch (RuntimeException exception) {
            rethrowIfInterrupted(exception);
            metrics.failed(failureId);
            log.warn("TMDB 콘텐츠 자동완성 동기화에 실패했습니다. contentId={}, externalId={}",
                contentId, failureId, exception);
        }
    }

    private static void rethrowIfInterrupted(RuntimeException exception) {
        if (Thread.currentThread().isInterrupted()) throw exception;
    }

    private void saveEpisodes(
        Content season,
        JsonNode ko,
        JsonNode en,
        LocalDate runDate
    ) {
        List<JsonNode> episodes = iterable(ko.path("episodes"));
        Map<Integer, JsonNode> english = iterable(en.path("episodes")).stream()
            .collect(java.util.stream.Collectors.toMap(node -> node.path("id").asInt(), node -> node, (a, b) -> a));
        Set<Integer> episodeIds = episodes.stream()
            .map(episode -> episode.path("id").asInt())
            .filter(id -> id > 0)
            .collect(java.util.stream.Collectors.toSet());
        Set<Integer> knownEpisodeIds = episodeIds.isEmpty()
            ? new HashSet<>()
            : episodeRepository.findAllByExternalSourceAndExternalIdIn(SOURCE, episodeIds).stream()
                .map(Episode::getExternalId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<Episode> existingEpisodes =
            episodeRepository.findAllBySeason_IdAndSeason_HiddenFalseOrderByEpisodeNumberAsc(
                season.getId()
            );
        Set<Integer> knownEpisodeNumbers = existingEpisodes.stream()
            .map(Episode::getEpisodeNumber)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        for (JsonNode episode : episodes) {
            LocalDate airDate = date(text(episode, "air_date", null));
            if (airDate == null || airDate.isAfter(runDate)) continue;
            int id = episode.path("id").asInt();
            int number = episode.path("episode_number").asInt();
            if (id <= 0 || number < 0
                || knownEpisodeIds.contains(id) || knownEpisodeNumbers.contains(number)) continue;
            JsonNode englishEpisode = english.getOrDefault(id, episode);
            String title = limit(firstText(episode, englishEpisode, "name"), 255);
            String description = firstText(episode, englishEpisode, "overview");
            String thumbnailUrl = properties.imageUrl(firstText(
                episode, englishEpisode, "still_path"));
            Integer runtime = positiveInt(episode.path("runtime"));
            if (title == null) {
                title = season.getSeasonNumber() == 0 ? "스페셜 " + number + "화" : number + "화";
            }
            episodeRepository.save(Episode.builder()
                .season(season).episodeNumber(number).title(title)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .runtime(runtime)
                .externalSource(SOURCE).externalId(id).build());
            knownEpisodeIds.add(id);
            knownEpisodeNumbers.add(number);
        }
    }

    private static void updateEpisodeCount(Content season, int remoteEpisodeCount) {
        int currentEpisodeCount = season.getEpisodeCount() == null ? 0 : season.getEpisodeCount();
        if (remoteEpisodeCount > currentEpisodeCount) {
            season.updateTvSeasonDetails(
                season.getParentContent(),
                season.getSeasonNumber(),
                remoteEpisodeCount,
                season.getRuntime()
            );
        }
    }

    private static int remoteEpisodeCount(SeasonData data) {
        return Math.max(
            data.candidate().path("episode_count").asInt(),
            data.ko().path("episodes").size()
        );
    }

    private void saveGenres(Content content, JsonNode genres) {
        for (JsonNode value : genres) {
            int id = value.path("id").asInt();
            String name = limit(text(value, "name", null), 50);
            if (id <= 0 || name == null) continue;
            Genre genre = genreRepository.findByExternalSourceAndExternalId(SOURCE, id)
                .or(() -> genreRepository.findByName(name))
                .orElseGet(() -> genreRepository.save(Genre.create(name, SOURCE, id)));
            contentGenreRepository.save(ContentGenre.create(content, genre));
        }
    }

    private static boolean hasValidGenres(JsonNode genres) {
        if (!genres.isArray()) return false;
        for (JsonNode genre : genres) {
            if (genre.path("id").asInt() > 0 && text(genre, "name", null) != null) return true;
        }
        return false;
    }

    private void saveCast(Content content, JsonNode cast) {
        int order = 0;
        Set<String> names = new HashSet<>();
        for (JsonNode person : cast) {
            String name = limit(text(person, "name", null), 100);
            if (name == null || !names.add(name) || order >= CAST_LIMIT) continue;
            String role = text(person, "character", null);
            if (role == null && person.path("roles").isArray() && !person.path("roles").isEmpty()) {
                role = text(person.path("roles").get(0), "character", null);
            }
            contentCastRepository.save(ContentCast.create(content, name, order++, limit(role, 255),
                properties.imageUrl(text(person, "profile_path", null))));
        }
    }

    private static boolean needsFallback(JsonNode node, String... fields) {
        for (String field : fields) if (text(node, field, null) == null) return true;
        return false;
    }
    private static String firstText(JsonNode primary, JsonNode fallback, String field) {
        String value = text(primary, field, null);
        return value != null ? value : text(fallback, field, null);
    }
    private static String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("").strip();
        return value.isEmpty() ? fallback : value;
    }
    private static LocalDate date(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); } catch (RuntimeException ignored) { return null; }
    }
    private boolean shouldLoadSeasonDetails(
        JsonNode candidate,
        Content existing,
        boolean sourceSeriesHidden,
        Set<Integer> existingSeasonNumbers,
        Set<Integer> episodeSeasonNumbers,
        Set<Integer> seasonNumbersToRefresh,
        LocalDate runDate
    ) {
        if (existing == null) {
            int seasonNumber = candidate.path("season_number").asInt();
            return !sourceSeriesHidden
                && !existingSeasonNumbers.contains(seasonNumber)
                && (isSeasonReleased(candidate, runDate)
                    || episodeSeasonNumbers.contains(seasonNumber));
        }
        return !existing.isHidden()
            && seasonNumbersToRefresh.contains(candidate.path("season_number").asInt());
    }

    private static Set<Integer> episodeSeasonNumbers(JsonNode series, LocalDate runDate) {
        Set<Integer> seasonNumbers = new HashSet<>();
        JsonNode lastEpisode = series.path("last_episode_to_air");
        if (lastEpisode.hasNonNull("season_number")) {
            int seasonNumber = lastEpisode.path("season_number").asInt(-1);
            if (seasonNumber >= 0) seasonNumbers.add(seasonNumber);
        }

        JsonNode nextEpisode = series.path("next_episode_to_air");
        LocalDate nextAirDate = date(text(nextEpisode, "air_date", null));
        if (nextEpisode.hasNonNull("season_number")
            && nextAirDate != null
            && !nextAirDate.isAfter(runDate)) {
            int seasonNumber = nextEpisode.path("season_number").asInt(-1);
            if (seasonNumber >= 0) seasonNumbers.add(seasonNumber);
        }
        return seasonNumbers;
    }

    private static Set<Integer> refreshSeasonNumbers(
        Set<Integer> episodeSeasonNumbers,
        List<JsonNode> seasons,
        LocalDate runDate
    ) {
        Set<Integer> seasonNumbers = new HashSet<>(episodeSeasonNumbers);

        seasons.stream()
            .filter(season -> season.path("season_number").asInt(-1) > 0)
            .filter(season -> isReleasedSeason(season, runDate))
            .sorted(Comparator.comparing(
                (JsonNode season) -> date(text(season, "air_date", null)),
                Comparator.reverseOrder()
            ))
            .limit(2)
            .map(season -> season.path("season_number").asInt())
            .forEach(seasonNumbers::add);

        seasons.stream()
            .filter(season -> season.path("season_number").asInt(-1) == 0)
            .filter(season -> isReleasedSeason(season, runDate))
            .findFirst()
            .ifPresent(season -> seasonNumbers.add(0));
        return seasonNumbers;
    }

    private static boolean isReleasedSeason(JsonNode season, LocalDate runDate) {
        return isSeasonReleased(season, runDate)
            && season.path("episode_count").asInt() > 0;
    }

    private static boolean isSeasonReleased(JsonNode season, LocalDate runDate) {
        LocalDate airDate = date(text(season, "air_date", null));
        return airDate != null && !airDate.isAfter(runDate);
    }
    private static Integer positiveInt(JsonNode value) {
        int number = value.asInt();
        return number > 0 ? number : null;
    }
    private static LocalDate koreanMovieReleaseDate(JsonNode ko, JsonNode en) {
        for (JsonNode response : List.of(ko, en)) {
            for (JsonNode country : response.path("release_dates").path("results")) {
                if (!"KR".equals(country.path("iso_3166_1").asText())) continue;
                for (int type : List.of(2, 3, 4, 6)) {
                    for (JsonNode release : country.path("release_dates")) {
                        if (release.path("type").asInt() != type) continue;
                        String value = text(release, "release_date", null);
                        if (value != null && value.length() >= 10) return date(value.substring(0, 10));
                    }
                }
            }
        }
        return date(firstText(ko, en, "release_date"));
    }
    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
    private static List<JsonNode> iterable(JsonNode array) {
        if (!array.isArray()) return List.of();
        java.util.ArrayList<JsonNode> values = new java.util.ArrayList<>();
        array.forEach(values::add);
        return values;
    }

    private record SeasonData(
        JsonNode candidate,
        JsonNode ko,
        JsonNode en,
        boolean existing,
        boolean hidden
    ) { }

    private record SeasonSaveResult(Content season, int createdCount) {
        private static SeasonSaveResult empty() {
            return new SeasonSaveResult(null, 0);
        }
    }

}
