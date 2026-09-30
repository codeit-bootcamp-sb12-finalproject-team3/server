package com.moduplaylist.batch.job.contentimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.moduplaylist.batch.job.contentimport.ContentImportMetrics.TmdbMovieProviderRetryTarget;
import com.moduplaylist.batch.job.contentimport.ContentImportMetrics.TmdbProviderRetryTarget;
import com.moduplaylist.batch.job.contenttagging.TmdbKeywordService;
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
import com.moduplaylist.infrastructure.externalapi.ExternalApiException;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSource;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import com.moduplaylist.infrastructure.opensearch.content.OpenSearchFailureClassifier;
import com.moduplaylist.infrastructure.tmdb.TmdbContentClient;
import com.moduplaylist.infrastructure.tmdb.TmdbProperties;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderClient;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
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
    private final TmdbKeywordService keywordService;
    private final TmdbWatchProviderClient watchProviderClient;
    private final TmdbContentPlatformService contentPlatformService;
    private final TmdbProperties properties;
    private final ContentRepository contentRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;
    private final ContentGenreRepository contentGenreRepository;
    private final ContentCastRepository contentCastRepository;
    private final ContentIndexSynchronizer contentIndexSynchronizer;
    private final TransactionTemplate transactionTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    public void importMovies(LocalDate runDate, ContentImportMetrics metrics) {
        LocalDate from = runDate.minusDays(1);
        Set<Integer> handledIds = new HashSet<>();
        Set<UUID> attemptedProviderContentIds = new HashSet<>();
        IndexSyncAttempts indexSyncAttempts = new IndexSyncAttempts();
        GenreCache genreCache = loadGenreCache();
        retryFailedMovieImports(
            handledIds,
            attemptedProviderContentIds,
            indexSyncAttempts,
            genreCache,
            metrics
        );
        retryFailedMovieSynchronizations(
            metrics,
            attemptedProviderContentIds,
            indexSyncAttempts
        );
        Set<Integer> ids = new HashSet<>();
        int totalPages = 1;
        for (int page = 1; page <= totalPages; page++) {
            try {
                JsonNode response = tmdbClient.discoverMovies(from, runDate, page);
                response.path("results").forEach(candidate -> {
                    int id = candidate.path("id").asInt();
                    if (id > 0 && !handledIds.contains(id) && ids.add(id)) metrics.candidate();
                });
                totalPages = response.path("total_pages").asInt(1);
            } catch (RuntimeException exception) {
                rethrowIfFatal(exception);
                metrics.failed("movie-discover:page-" + page);
                log.warn("TMDB 영화 목록 조회에 실패해 다음 페이지를 처리합니다. page={}",
                    page, exception);
            }
        }
        Map<Integer, Content> existingMovies = findExistingContentsByExternalId(
            ContentType.MOVIE, ids);
        ids.forEach(id -> fetchAndImportMovieSafely(
            id,
            existingMovies.get(id),
            attemptedProviderContentIds,
            indexSyncAttempts,
            genreCache,
            metrics
        ));
    }

    public void importTvSeasons(LocalDate runDate, ContentImportMetrics metrics) {
        LocalDate from = runDate.minusDays(1);
        Set<Integer> handledIds = new HashSet<>();
        Set<UUID> attemptedProviderContentIds = new HashSet<>();
        IndexSyncAttempts indexSyncAttempts = new IndexSyncAttempts();
        GenreCache genreCache = loadGenreCache();
        retryFailedTvImports(
            handledIds,
            runDate,
            attemptedProviderContentIds,
            indexSyncAttempts,
            genreCache,
            metrics
        );
        retryFailedSeasonSynchronizations(
            metrics,
            attemptedProviderContentIds,
            indexSyncAttempts
        );
        Set<Integer> ids = new HashSet<>();
        collectTvCandidates(ids, handledIds, metrics, "networks",
            page -> tmdbClient.discoverTvByNetworks(from, runDate, page));
        collectTvCandidates(ids, handledIds, metrics, "watch-providers",
            page -> tmdbClient.discoverTvByWatchProviders(from, runDate, page));
        Map<Integer, Content> existingSeries = findExistingContentsByExternalId(
            ContentType.TV_SERIES, ids);
        ids.forEach(id -> importSeriesSeasonsSafely(
            id,
            existingSeries.get(id),
            runDate,
            attemptedProviderContentIds,
            indexSyncAttempts,
            genreCache,
            metrics
        ));
    }

    private void collectTvCandidates(
        Set<Integer> ids,
        Set<Integer> handledIds,
        ContentImportMetrics metrics,
        String discoverySource,
        IntFunction<JsonNode> requestPage
    ) {
        int totalPages = 1;
        for (int page = 1; page <= totalPages; page++) {
            try {
                JsonNode response = requestPage.apply(page);
                response.path("results").forEach(candidate -> {
                    int id = candidate.path("id").asInt();
                    if (id > 0 && !handledIds.contains(id) && ids.add(id)) metrics.candidate();
                });
                totalPages = response.path("total_pages").asInt(1);
            } catch (RuntimeException exception) {
                rethrowIfFatal(exception);
                metrics.failed("tv-discover:" + discoverySource + ":page-" + page);
                log.warn(
                    "TMDB TV 목록 조회에 실패해 다음 페이지를 처리합니다. source={}, page={}",
                    discoverySource, page, exception
                );
            }
        }
    }

    private void fetchAndImportMovieSafely(
        int id,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        try {
            Content existing = contentRepository.findByExternalSourceAndTypeAndExternalId(
                SOURCE, ContentType.MOVIE, id).orElse(null);
            fetchAndImportMovie(
                id,
                existing,
                attemptedProviderContentIds,
                indexSyncAttempts,
                genreCache,
                metrics
            );
            metrics.completeMovieRetry(id);
        } catch (RuntimeException exception) {
            rethrowIfFatal(exception);
            metrics.addMovieRetry(id);
            metrics.failed("movie:" + id);
            log.warn("TMDB 영화 수집에 실패해 다음 콘텐츠를 처리합니다. tmdbId={}", id, exception);
        }
    }

    private void fetchAndImportMovieSafely(
        int id,
        Content existing,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        try {
            fetchAndImportMovie(
                id,
                existing,
                attemptedProviderContentIds,
                indexSyncAttempts,
                genreCache,
                metrics
            );
            metrics.completeMovieRetry(id);
        } catch (RuntimeException exception) {
            rethrowIfFatal(exception);
            metrics.addMovieRetry(id);
            metrics.failed("movie:" + id);
            log.warn("TMDB 영화 수집에 실패해 다음 콘텐츠를 처리합니다. tmdbId={}", id, exception);
        }
    }

    private void importSeriesSeasonsSafely(
        int seriesId,
        LocalDate runDate,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        try {
            Content series = contentRepository.findByExternalSourceAndTypeAndExternalId(
                SOURCE, ContentType.TV_SERIES, seriesId).orElse(null);
            boolean failed = importSeriesSeasons(
                seriesId,
                series,
                runDate,
                attemptedProviderContentIds,
                indexSyncAttempts,
                genreCache,
                metrics
            );
            if (!failed) metrics.completeTvSeriesRetry(seriesId);
        } catch (RuntimeException exception) {
            rethrowIfFatal(exception);
            metrics.addTvSeriesRetry(seriesId);
            metrics.failed("tv-series:" + seriesId);
            log.warn("TMDB TV 시리즈 조회에 실패해 다음 콘텐츠를 처리합니다. tmdbId={}",
                seriesId, exception);
        }
    }

    private void importSeriesSeasonsSafely(
        int seriesId,
        Content series,
        LocalDate runDate,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        try {
            boolean failed = importSeriesSeasons(
                seriesId,
                series,
                runDate,
                attemptedProviderContentIds,
                indexSyncAttempts,
                genreCache,
                metrics
            );
            if (!failed) metrics.completeTvSeriesRetry(seriesId);
        } catch (RuntimeException exception) {
            rethrowIfFatal(exception);
            metrics.addTvSeriesRetry(seriesId);
            metrics.failed("tv-series:" + seriesId);
            log.warn("TMDB TV 시리즈 조회에 실패해 다음 콘텐츠를 처리합니다. tmdbId={}",
                seriesId, exception);
        }
    }

    private void fetchAndImportMovie(
        int id,
        Content existing,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        if (existing != null) {
            metrics.existing();
            synchronizeMoviePostProcessing(
                existing,
                id,
                attemptedProviderContentIds,
                indexSyncAttempts,
                metrics
            );
            return;
        }
        JsonNode ko = tmdbClient.movieDetails(id, "ko-KR");
        JsonNode en = tmdbClient.movieDetails(id, "en-US");
        String title = limit(firstText(ko, en, "title"), 255);
        String description = firstText(ko, en, "overview");
        JsonNode genres = hasValidGenres(ko.path("genres"))
            ? ko.path("genres") : en.path("genres");
        if (title == null || description == null || !hasValidGenres(genres)) {
            metrics.missingRequired();
            log.info("TMDB 영화 필수 정보가 없어 건너뜁니다. tmdbId={}", id);
            return;
        }
        Map<String, Object> keywordMetadata = keywordService.fetch(id, false);
        MovieSaveResult result = transactionTemplate.execute(status ->
            importMovie(id, ko, en, genres, genreCache, keywordMetadata));
        if (result == null) {
            metrics.existing();
            contentRepository.findByExternalSourceAndTypeAndExternalId(
                    SOURCE, ContentType.MOVIE, id)
                .ifPresent(content -> {
                    synchronizeMoviePostProcessing(
                        content,
                        id,
                        attemptedProviderContentIds,
                        indexSyncAttempts,
                        metrics
                    );
                });
            return;
        }
        genreCache.putAll(result.genreResolutions());
        Content movie = result.movie();
        metrics.created();
        synchronizeMoviePostProcessing(
            movie,
            id,
            attemptedProviderContentIds,
            indexSyncAttempts,
            metrics
        );
    }

    private MovieSaveResult importMovie(
        int id,
        JsonNode ko,
        JsonNode en,
        JsonNode genres,
        GenreCache genreCache,
        Map<String, Object> keywordMetadata
    ) {
        if (contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.MOVIE, id).isPresent()) return null;
        String title = limit(firstText(ko, en, "title"), 255);
        String description = firstText(ko, en, "overview");
        String originalTitle = limit(text(ko, "original_title", title), 255);
        String englishTitle = limit(text(en, "title", originalTitle), 255);
        Content movie = Content.builder()
            .title(title).type(ContentType.MOVIE).description(description)
            .thumbnailUrl(properties.imageUrl(firstText(ko, en, "poster_path")))
            .releaseDate(koreanMovieReleaseDate(ko, en))
            .runtime(positiveInt(ko.path("runtime")))
            .metadata(titleMetadata(originalTitle, englishTitle))
            .externalSource(SOURCE).externalId(id).build();
        movie.updateAiTaggingStatus(AiTaggingStatus.PENDING);
        movie.mergeMetadata(keywordMetadata);
        contentRepository.save(movie);
        List<GenreResolution> genreResolutions = saveGenres(movie, genres, genreCache);
        saveCast(movie, ko.path("credits").path("cast"));
        return new MovieSaveResult(movie, genreResolutions);
    }

    private boolean importSeriesSeasons(
        int seriesId,
        Content series,
        LocalDate runDate,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        if (series != null) metrics.existing();
        boolean sourceSeriesHidden = series != null && series.isHidden();
        JsonNode ko = tmdbClient.tvDetails(seriesId, "ko-KR");

        List<JsonNode> remoteSeasons = iterable(ko.path("seasons")).stream()
            .filter(candidate -> candidate.hasNonNull("id") && candidate.hasNonNull("season_number"))
            .filter(candidate -> candidate.path("season_number").asInt() >= 0)
            .toList();
        if (remoteSeasons.isEmpty()) return false;

        Map<Integer, Content> existingSeasons = findExistingSeasonsByExternalId(remoteSeasons);
        boolean hasMissingRemoteSeason = remoteSeasons.stream().anyMatch(candidate ->
            !existingSeasons.containsKey(candidate.path("id").asInt()));
        Set<Integer> existingSeasonNumbers = series != null
            && !sourceSeriesHidden
            && hasMissingRemoteSeason
                ? new HashSet<>(
                    contentRepository.findAllSeasonNumbersByParentContentId(series.getId()))
                : Set.of();
        synchronizeExistingSeasons(
            existingSeasons.values(),
            indexSyncAttempts,
            metrics
        );
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
        if (candidates.isEmpty()) return false;

        JsonNode en = tmdbClient.tvDetails(seriesId, "en-US");
        if (series == null && firstText(ko, en, "name") == null
            && text(ko, "original_name", null) == null) {
            metrics.missingRequired();
            return false;
        }
        JsonNode genres = hasValidGenres(ko.path("genres"))
            ? ko.path("genres") : en.path("genres");
        Map<String, Object> keywordMetadata = hasMissingRemoteSeason
            && (series == null || !TmdbKeywordService.successful(series.getMetadata()))
            ? keywordService.fetch(seriesId, true) : Map.of();
        boolean failed = false;
        for (JsonNode candidate : candidates) {
            int number = candidate.path("season_number").asInt();
            String failureId = "tv-season:" + seriesId + "/" + number;
            try {
                JsonNode seasonKo = tmdbClient.seasonDetails(seriesId, number, "ko-KR");
                JsonNode seasonEn = tmdbClient.seasonDetails(seriesId, number, "en-US");
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
                    saveSeason(seriesId, data, ko, en, genres, runDate, genreCache, keywordMetadata)
                );
                if (result != null) genreCache.putAll(result.genreResolutions());
                if (result != null && result.season() != null) {
                    metrics.created(result.createdCount());
                    UUID contentId = result.season().getId();
                    TmdbProviderRetryTarget providerTarget = new TmdbProviderRetryTarget(
                        contentId, seriesId, number);
                    if (attemptedProviderContentIds.add(contentId)) {
                        try {
                            synchronizeSeasonProvidersSafely(providerTarget, failureId, metrics);
                        } catch (RuntimeException exception) {
                            addAllIndexRetries(contentId, metrics);
                            throw exception;
                        }
                    }
                    synchronizeIndexesOnce(contentId, failureId, indexSyncAttempts, metrics);
                }
            } catch (RuntimeException exception) {
                rethrowIfFatal(exception);
                metrics.addTvSeriesRetry(seriesId);
                failed = true;
                metrics.failed(failureId);
                log.warn("TMDB TV 시즌 수집에 실패해 다음 시즌을 처리합니다. seriesId={}, seasonNumber={}",
                    seriesId, number, exception);
            }
        }
        return failed;
    }

    private Map<Integer, Content> findExistingSeasonsByExternalId(List<JsonNode> remoteSeasons) {
        Set<Integer> externalIds = new HashSet<>();
        remoteSeasons.forEach(season -> externalIds.add(season.path("id").asInt()));
        return findExistingContentsByExternalId(ContentType.TV_SEASON, externalIds);
    }

    private Map<Integer, Content> findExistingContentsByExternalId(
        ContentType type,
        Collection<Integer> externalIds
    ) {
        if (externalIds.isEmpty()) return Map.of();

        Map<Integer, Content> existingContents = new LinkedHashMap<>();
        contentRepository.findAllByExternalSourceAndTypeAndExternalIdIn(
                SOURCE, type, externalIds)
            .forEach(content -> existingContents.put(content.getExternalId(), content));
        return existingContents;
    }

    private void synchronizeExistingSeasons(
        Collection<Content> seasons,
        IndexSyncAttempts indexSyncAttempts,
        ContentImportMetrics metrics
    ) {
        for (Content season : seasons) {
            if (season.isHidden()) continue;
            metrics.existing();
            UUID contentId = season.getId();
            synchronizeIndexesOnce(
                contentId,
                "tv-season-index:" + contentId,
                indexSyncAttempts,
                metrics
            );
        }
    }

    private static boolean missingSeasonRequired(SeasonData data, JsonNode genres) {
        return firstText(data.ko(), data.en(), "overview") == null
            || !hasValidGenres(genres);
    }

    private SeasonSaveResult saveSeason(
        int seriesId,
        SeasonData data,
        JsonNode seriesKo,
        JsonNode seriesEn,
        JsonNode genres,
        LocalDate runDate,
        GenreCache genreCache,
        Map<String, Object> keywordMetadata
    ) {
        Content series = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SERIES, seriesId).orElse(null);
        if (series != null) {
            // Refresh the entity read above after taking the parent lock to preserve concurrent metadata edits.
            entityManager.refresh(series, LockModeType.PESSIMISTIC_WRITE);
        }
        int seasonId = data.candidate().path("id").asInt();
        Content existing = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SEASON, seasonId).orElse(null);
        if (existing != null) {
            if (existing.isHidden()) return SeasonSaveResult.empty();
            saveEpisodes(existing, data.ko(), data.en(), runDate);
            updateEpisodeCount(existing, remoteEpisodeCount(data));
            return new SeasonSaveResult(existing, 0, List.of());
        }
        if (series != null && series.isHidden()) return SeasonSaveResult.empty();
        if (series != null && contentRepository.findByParentContent_IdAndSeasonNumber(
            series.getId(), data.candidate().path("season_number").asInt()).isPresent()) {
            return SeasonSaveResult.empty();
        }
        if (series == null) {
            String seriesTitle = limit(firstText(seriesKo, seriesEn, "name"), 255);
            if (seriesTitle == null) {
                seriesTitle = limit(text(seriesKo, "original_name", null), 255);
            }
            if (seriesTitle == null) return SeasonSaveResult.empty();
            String originalTitle = limit(
                text(seriesKo, "original_name", seriesTitle), 255);
            String englishTitle = limit(text(seriesEn, "name", originalTitle), 255);
            series = contentRepository.save(Content.builder()
                .title(seriesTitle).type(ContentType.TV_SERIES)
                .metadata(titleMetadata(originalTitle, englishTitle))
                .externalSource(SOURCE).externalId(seriesId).build());
        }
        if (!keywordMetadata.isEmpty() && !TmdbKeywordService.successful(series.getMetadata())) {
            series.mergeMetadata(keywordMetadata);
        }
        Content season = importSeason(series, data, seriesKo, seriesEn, runDate);
        if (season == null) return SeasonSaveResult.empty();
        List<GenreResolution> genreResolutions = saveGenres(season, genres, genreCache);
        saveCast(season, seasonCast(data, seriesKo, seriesEn));
        return new SeasonSaveResult(season, 1, genreResolutions);
    }

    private Content importSeason(
        Content series,
        SeasonData data,
        JsonNode seriesKo,
        JsonNode seriesEn,
        LocalDate runDate
    ) {
        JsonNode candidate = data.candidate();
        int number = candidate.path("season_number").asInt();
        JsonNode ko = data.ko();
        JsonNode en = data.en();
        String seasonName = firstText(ko, en, "name");
        String resolvedSeasonName = seasonName != null
            ? seasonName
            : number == 0 ? "스페셜" : "시즌 " + number;
        String title = limit(series.getTitle() + " " + resolvedSeasonName, 255);
        String description = firstText(ko, en, "overview");
        String originalTitle = limit(
            text(seriesKo, "original_name", series.getTitle()), 255);
        String englishTitle = limit(text(seriesEn, "name", originalTitle), 255);
        int seasonId = candidate.path("id").asInt();
        Content season = Content.builder()
            .parentContent(series).title(title).seasonNumber(number)
            .episodeCount(remoteEpisodeCount(data)).type(ContentType.TV_SEASON)
            .description(description).thumbnailUrl(properties.imageUrl(firstText(ko, en, "poster_path")))
            .releaseDate(date(firstText(ko, en, "air_date")))
            .metadata(titleMetadata(originalTitle, englishTitle))
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

    private void synchronizeMoviePostProcessing(
        Content movie,
        int movieId,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        ContentImportMetrics metrics
    ) {
        UUID contentId = movie.getId();
        TmdbMovieProviderRetryTarget providerTarget = new TmdbMovieProviderRetryTarget(
            contentId, movieId);
        if (!movie.isHidden() && attemptedProviderContentIds.add(contentId)) {
            try {
                synchronizeMovieProvidersSafely(providerTarget, metrics);
            } catch (RuntimeException exception) {
                addAllIndexRetries(contentId, metrics);
                throw exception;
            }
        }
        synchronizeIndexesOnce(
            contentId,
            "movie:" + movieId,
            indexSyncAttempts,
            metrics
        );
    }

    private void synchronizeMovieProvidersSafely(
        TmdbMovieProviderRetryTarget target,
        ContentImportMetrics metrics
    ) {
        try {
            TmdbWatchProviderResponse providers = watchProviderClient.fetchMovie(target.movieId());
            contentPlatformService.synchronizeProviders(target.contentId(), providers);
            metrics.completeTmdbMovieProviderRetry(target);
        } catch (RuntimeException exception) {
            metrics.addTmdbMovieProviderRetry(target);
            rethrowIfFatal(exception);
            metrics.failed("movie:" + target.movieId());
            log.warn("TMDB 영화 OTT 제공처 동기화에 실패했습니다. tmdbId={}",
                target.movieId(), exception);
        }
    }

    private void synchronizeSeasonProvidersSafely(
        TmdbProviderRetryTarget target,
        String failureId,
        ContentImportMetrics metrics
    ) {
        try {
            TmdbWatchProviderResponse providers = watchProviderClient.fetchTvSeason(
                target.seriesId(), target.seasonNumber());
            contentPlatformService.synchronizeProviders(target.contentId(), providers);
            metrics.completeTmdbProviderRetry(target);
        } catch (RuntimeException exception) {
            metrics.addTmdbProviderRetry(target);
            rethrowIfFatal(exception);
            metrics.failed(failureId);
            log.warn(
                "TMDB 시즌 OTT 제공처 동기화에 실패했습니다. seriesId={}, seasonNumber={}",
                target.seriesId(), target.seasonNumber(), exception
            );
        }
    }

    private void synchronizeIndexesOnce(
        UUID contentId,
        String failureId,
        IndexSyncAttempts attempts,
        ContentImportMetrics metrics
    ) {
        boolean synchronizeAutocomplete = attempts.autocomplete().add(contentId);
        boolean synchronizeSearch = attempts.search().add(contentId);
        synchronizeIndexesSafely(
            contentId,
            failureId,
            synchronizeAutocomplete,
            synchronizeSearch,
            metrics
        );
    }

    private void synchronizeIndexesSafely(
        UUID contentId,
        String failureId,
        boolean synchronizeAutocomplete,
        boolean synchronizeSearch,
        ContentImportMetrics metrics
    ) {
        if (!synchronizeAutocomplete && !synchronizeSearch) return;
        boolean taggingPending = contentRepository.findById(contentId)
            .map(content -> content.getAiTaggingStatus() == AiTaggingStatus.PENDING)
            .orElse(false);
        if (taggingPending) {
            if (synchronizeAutocomplete) metrics.completeAutocompleteRetry(contentId);
            if (synchronizeSearch) metrics.completeSearchRetry(contentId);
            log.debug("AI 태깅 전 TMDB 콘텐츠 색인을 보류합니다. contentId={}", contentId);
            return;
        }

        ContentIndexSource source;
        try {
            source = contentIndexSynchronizer.load(contentId);
        } catch (RuntimeException exception) {
            addIndexRetries(contentId, synchronizeAutocomplete, synchronizeSearch, metrics);
            metrics.failed(failureId + ":index-source");
            log.warn("TMDB 색인 원본 조회에 실패했습니다. contentId={}, externalId={}",
                contentId, failureId, exception);
            rethrowIfFatal(exception);
            return;
        }

        RuntimeException fatalFailure = null;
        if (synchronizeAutocomplete) {
            try {
                contentIndexSynchronizer.synchronizeAutocomplete(source);
                metrics.completeAutocompleteRetry(contentId);
            } catch (RuntimeException exception) {
                metrics.addAutocompleteRetry(contentId);
                metrics.failed(failureId + ":autocomplete");
                log.warn("TMDB 자동완성 동기화에 실패했습니다. contentId={}, externalId={}",
                    contentId, failureId, exception);
                fatalFailure = fatalFailure(fatalFailure, exception);
            }
        }
        if (synchronizeSearch) {
            try {
                contentIndexSynchronizer.synchronizeSearch(source);
                metrics.completeSearchRetry(contentId);
            } catch (RuntimeException exception) {
                metrics.addSearchRetry(contentId);
                metrics.failed(failureId + ":search");
                log.warn("TMDB 검색 문서 동기화에 실패했습니다. contentId={}, externalId={}",
                    contentId, failureId, exception);
                fatalFailure = fatalFailure(fatalFailure, exception);
            }
        }
        if (fatalFailure != null) throw fatalFailure;
    }

    private static RuntimeException fatalFailure(
        RuntimeException current,
        RuntimeException candidate
    ) {
        if (!isFatal(candidate)) return current;
        if (current == null) return candidate;
        current.addSuppressed(candidate);
        return current;
    }

    private static boolean isFatal(RuntimeException exception) {
        return OpenSearchFailureClassifier.shouldAbortStep(exception);
    }

    private static void addAllIndexRetries(
        UUID contentId,
        ContentImportMetrics metrics
    ) {
        addIndexRetries(contentId, true, true, metrics);
    }

    private static void addIndexRetries(
        UUID contentId,
        boolean autocomplete,
        boolean search,
        ContentImportMetrics metrics
    ) {
        if (autocomplete) metrics.addAutocompleteRetry(contentId);
        if (search) metrics.addSearchRetry(contentId);
    }

    private void retryFailedMovieSynchronizations(
        ContentImportMetrics metrics,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts
    ) {
        for (TmdbMovieProviderRetryTarget target : metrics.tmdbMovieProviderRetryTargets()) {
            attemptedProviderContentIds.add(target.contentId());
            synchronizeMovieProvidersSafely(target, metrics);
        }
        retryFailedIndexes(metrics, indexSyncAttempts, "movie-index:");
    }

    private void retryFailedMovieImports(
        Set<Integer> handledIds,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        for (Integer movieId : metrics.movieRetryIds()) {
            if (!handledIds.add(movieId)) continue;
            fetchAndImportMovieSafely(
                movieId,
                attemptedProviderContentIds,
                indexSyncAttempts,
                genreCache,
                metrics
            );
        }
    }

    private void retryFailedSeasonSynchronizations(
        ContentImportMetrics metrics,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts
    ) {
        for (TmdbProviderRetryTarget target : metrics.tmdbProviderRetryTargets()) {
            attemptedProviderContentIds.add(target.contentId());
            String failureId = "tv-season:" + target.seriesId() + "/" + target.seasonNumber();
            synchronizeSeasonProvidersSafely(target, failureId, metrics);
        }
        retryFailedIndexes(metrics, indexSyncAttempts, "tv-season-index:");
    }

    private void retryFailedIndexes(
        ContentImportMetrics metrics,
        IndexSyncAttempts attempts,
        String failureIdPrefix
    ) {
        Set<UUID> autocompleteRetries = metrics.autocompleteRetryContentIds();
        Set<UUID> searchRetries = metrics.searchRetryContentIds();
        Set<UUID> contentIds = new LinkedHashSet<>(autocompleteRetries);
        contentIds.addAll(searchRetries);

        for (UUID contentId : contentIds) {
            boolean synchronizeAutocomplete = autocompleteRetries.contains(contentId)
                && attempts.autocomplete().add(contentId);
            boolean synchronizeSearch = searchRetries.contains(contentId)
                && attempts.search().add(contentId);
            synchronizeIndexesSafely(
                contentId,
                failureIdPrefix + contentId,
                synchronizeAutocomplete,
                synchronizeSearch,
                metrics
            );
        }
    }

    private void retryFailedTvImports(
        Set<Integer> handledIds,
        LocalDate runDate,
        Set<UUID> attemptedProviderContentIds,
        IndexSyncAttempts indexSyncAttempts,
        GenreCache genreCache,
        ContentImportMetrics metrics
    ) {
        for (Integer seriesId : metrics.tvSeriesRetryIds()) {
            if (!handledIds.add(seriesId)) continue;
            importSeriesSeasonsSafely(
                seriesId,
                runDate,
                attemptedProviderContentIds,
                indexSyncAttempts,
                genreCache,
                metrics
            );
        }
    }

    private static void rethrowIfFatal(RuntimeException exception) {
        if (Thread.currentThread().isInterrupted()) {
            throw exception;
        }
        if (!(exception instanceof ExternalApiException externalApiException)) {
            throw exception;
        }
        if (externalApiException.isFatal()) {
            throw exception;
        }
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
        boolean episodeCreated = false;
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
            episodeCreated = true;
        }
        if (episodeCreated) {
            season.markUpdated();
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

    private GenreCache loadGenreCache() {
        GenreCache cache = new GenreCache();
        genreRepository.findAll().forEach(cache::putStored);
        return cache;
    }

    private List<GenreResolution> saveGenres(
        Content content,
        JsonNode genres,
        GenreCache genreCache
    ) {
        List<GenreResolution> newGenreResolutions = new ArrayList<>();
        for (JsonNode value : genres) {
            int id = value.path("id").asInt();
            String name = limit(text(value, "name", null), 50);
            if (id <= 0 || name == null) continue;
            Genre genre = genreCache.find(id, name);
            if (genre == null) {
                genre = genreRepository.findByExternalSourceAndExternalId(SOURCE, id)
                    .orElse(null);
                if (genre != null) {
                    genreCache.putResolved(id, name, genre);
                } else {
                    genre = genreRepository.findByName(name).orElse(null);
                    if (genre != null) {
                        genreCache.putResolved(id, name, genre);
                    } else {
                        genre = genreRepository.save(Genre.create(name, SOURCE, id));
                        newGenreResolutions.add(new GenreResolution(id, name, genre));
                    }
                }
            }
            contentGenreRepository.save(ContentGenre.create(content, genre));
        }
        return newGenreResolutions;
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
    private static Map<String, Object> titleMetadata(
        String originalTitle,
        String englishTitle
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (originalTitle != null) metadata.put("originalTitle", originalTitle);
        if (englishTitle != null) metadata.put("englishTitle", englishTitle);
        return Map.copyOf(metadata);
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

    private record GenreResolution(int externalId, String name, Genre genre) { }

    private record MovieSaveResult(Content movie, List<GenreResolution> genreResolutions) { }

    private record SeasonSaveResult(
        Content season,
        int createdCount,
        List<GenreResolution> genreResolutions
    ) {
        private static SeasonSaveResult empty() {
            return new SeasonSaveResult(null, 0, List.of());
        }
    }

    private record IndexSyncAttempts(
        Set<UUID> autocomplete,
        Set<UUID> search
    ) {
        private IndexSyncAttempts() {
            this(new HashSet<>(), new HashSet<>());
        }
    }

    private static final class GenreCache {
        private final Map<Integer, Genre> byExternalId = new LinkedHashMap<>();
        private final Map<String, Genre> byName = new LinkedHashMap<>();

        private Genre find(int externalId, String name) {
            Genre genre = byExternalId.get(externalId);
            return genre != null ? genre : byName.get(name);
        }

        private void putStored(Genre genre) {
            if (SOURCE.equals(genre.getExternalSource())) {
                byExternalId.put(genre.getExternalId(), genre);
            }
            byName.put(genre.getName(), genre);
        }

        private void putResolved(int externalId, String name, Genre genre) {
            byExternalId.put(externalId, genre);
            byName.put(name, genre);
        }

        private void putAll(Collection<GenreResolution> resolutions) {
            resolutions.forEach(resolution -> putResolved(
                resolution.externalId(),
                resolution.name(),
                resolution.genre()
            ));
        }
    }

}
