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
import com.moduplaylist.infrastructure.tmdb.TmdbContentClient;
import com.moduplaylist.infrastructure.tmdb.TmdbProperties;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderClient;
import com.moduplaylist.infrastructure.tmdb.TmdbWatchProviderResponse;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbContentImportService {
    private static final String SOURCE = "TMDB";
    private static final int PAGE_LIMIT = 5;
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
    private final TransactionTemplate transactionTemplate;

    public void importMovies(LocalDate runDate) {
        LocalDate from = runDate.minusDays(6);
        Set<Integer> ids = new HashSet<>();
        for (int page = 1; page <= PAGE_LIMIT; page++) {
            JsonNode response = tmdbClient.discoverMovies(from, runDate, page);
            response.path("results").forEach(candidate -> ids.add(candidate.path("id").asInt()));
            if (page >= Math.min(PAGE_LIMIT, response.path("total_pages").asInt())) break;
        }
        ids.forEach(this::fetchAndImportMovie);
    }

    public void importTvSeasons(LocalDate runDate) {
        LocalDate from = runDate.minusDays(6);
        Set<Integer> ids = new HashSet<>();
        for (int page = 1; page <= PAGE_LIMIT; page++) {
            JsonNode response = tmdbClient.discoverTv(from, runDate, page);
            response.path("results").forEach(candidate -> ids.add(candidate.path("id").asInt()));
            if (page >= Math.min(PAGE_LIMIT, response.path("total_pages").asInt())) break;
        }
        ids.forEach(id -> importSeriesSeasons(id, from, runDate));
    }

    private void fetchAndImportMovie(int id) {
        if (contentRepository.findByExternalSourceAndTypeAndExternalId(SOURCE, ContentType.MOVIE, id).isPresent()) return;
        JsonNode ko = tmdbClient.movieDetails(id, "ko-KR");
        JsonNode en = needsFallback(ko, "title", "overview", "poster_path")
            ? tmdbClient.movieDetails(id, "en-US") : ko;
        String title = limit(firstText(ko, en, "title"), 255);
        String description = firstText(ko, en, "overview");
        if (title == null || description == null) {
            log.info("TMDB 영화 필수 정보가 없어 건너뜁니다. tmdbId={}", id);
            return;
        }
        TmdbWatchProviderResponse providers = watchProviderClient.fetchMovie(id);
        transactionTemplate.executeWithoutResult(status -> importMovie(id, ko, en, providers));
    }

    private void importMovie(
        int id,
        JsonNode ko,
        JsonNode en,
        TmdbWatchProviderResponse providers
    ) {
        if (contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.MOVIE, id).isPresent()) return;
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
        saveGenres(movie, ko.path("genres"));
        saveCast(movie, ko.path("credits").path("cast"));
        contentPlatformService.saveInitialProviders(movie, providers);
    }

    private void importSeriesSeasons(int seriesId, LocalDate from, LocalDate to) {
        Content series = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SERIES, seriesId).orElse(null);
        if (series != null && series.isHidden()) return;
        JsonNode ko = tmdbClient.tvDetails(seriesId, "ko-KR");

        List<JsonNode> candidates = iterable(ko.path("seasons")).stream()
            .filter(candidate -> candidate.hasNonNull("id") && candidate.hasNonNull("season_number"))
            .filter(candidate -> candidate.path("season_number").asInt() >= 0)
            .filter(candidate -> shouldLoadSeasonDetails(candidate, from, to))
            .toList();
        if (candidates.isEmpty()) return;

        JsonNode en = needsFallback(ko, "name") ? tmdbClient.tvDetails(seriesId, "en-US") : ko;
        TmdbWatchProviderResponse providers = watchProviderClient.fetchTvSeries(seriesId);
        for (JsonNode candidate : candidates) {
            int number = candidate.path("season_number").asInt();
            JsonNode seasonKo = tmdbClient.seasonDetails(seriesId, number, "ko-KR");
            JsonNode seasonEn = tmdbClient.seasonDetails(seriesId, number, "en-US");
            JsonNode seasonOriginal = tmdbClient.seasonDetailsOriginal(seriesId, number);
            Content existing = contentRepository.findByExternalSourceAndTypeAndExternalId(
                SOURCE, ContentType.TV_SEASON, candidate.path("id").asInt()).orElse(null);
            SeasonData data = new SeasonData(
                candidate,
                seasonKo,
                seasonEn,
                seasonOriginal,
                existing != null,
                existing != null && existing.isHidden()
            );
            if (!isImportTarget(data, from, to)) continue;
            transactionTemplate.executeWithoutResult(status ->
                saveSeason(seriesId, data, ko, en, providers)
            );
        }
    }

    private void saveSeason(
        int seriesId,
        SeasonData data,
        JsonNode seriesKo,
        JsonNode seriesEn,
        TmdbWatchProviderResponse providers
    ) {
        Content series = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SERIES, seriesId).orElse(null);
        if (series != null && series.isHidden()) return;
        if (series == null) {
            String seriesTitle = limit(firstText(seriesKo, seriesEn, "name"), 255);
            if (seriesTitle == null) {
                seriesTitle = limit(text(seriesKo, "original_name", null), 255);
            }
            if (seriesTitle == null) return;
            series = contentRepository.save(Content.builder()
                .title(seriesTitle).type(ContentType.TV_SERIES)
                .metadata(Map.of("originalTitle", text(seriesKo, "original_name", seriesTitle)))
                .externalSource(SOURCE).externalId(seriesId).build());
        }

        int seasonId = data.candidate().path("id").asInt();
        Content existing = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.TV_SEASON, seasonId).orElse(null);
        if (existing != null) {
            if (existing.isHidden() || data.candidate().path("season_number").asInt() != 0) return;
            existing.updateTvSeasonDetails(
                series,
                0,
                data.ko().path("episodes").size(),
                existing.getRuntime()
            );
            saveEpisodes(existing, data.ko(), data.en(), data.original());
            return;
        }
        importSeason(series, data, seriesKo, seriesEn, providers);
    }

    private void importSeason(
        Content series,
        SeasonData data,
        JsonNode seriesKo,
        JsonNode seriesEn,
        TmdbWatchProviderResponse providers
    ) {
        JsonNode candidate = data.candidate();
        int number = candidate.path("season_number").asInt();
        JsonNode ko = data.ko();
        JsonNode en = data.en();
        JsonNode original = data.original();
        String title = limit(firstText(ko, en, original, "name"), 255);
        String description = firstText(ko, en, original, "overview");
        if (title == null || description == null) return;
        int seasonId = candidate.path("id").asInt();
        Content season = Content.builder()
            .parentContent(series).title(title).seasonNumber(number)
            .episodeCount(ko.path("episodes").size()).type(ContentType.TV_SEASON)
            .description(description).thumbnailUrl(properties.imageUrl(firstText(ko, en, "poster_path")))
            .releaseDate(date(firstText(ko, en, "air_date")))
            .metadata(Map.of("originalTitle", text(seriesKo, "original_name", title)))
            .externalSource(SOURCE).externalId(seasonId).build();
        season.updateAiTaggingStatus(AiTaggingStatus.PENDING);
        contentRepository.save(season);
        saveGenres(season, seriesKo.path("genres"));
        JsonNode cast = ko.path("aggregate_credits").path("cast");
        if (!cast.isArray() || cast.isEmpty()) cast = seriesKo.path("aggregate_credits").path("cast");
        if ((!cast.isArray() || cast.isEmpty()) && seriesEn != seriesKo) {
            cast = seriesEn.path("aggregate_credits").path("cast");
        }
        saveCast(season, cast);
        saveEpisodes(season, ko, en, original);
        contentPlatformService.saveInitialProviders(season, providers);
    }

    private void saveEpisodes(Content season, JsonNode ko, JsonNode en, JsonNode original) {
        List<JsonNode> episodes = iterable(ko.path("episodes"));
        Map<Integer, JsonNode> english = iterable(en.path("episodes")).stream()
            .collect(java.util.stream.Collectors.toMap(node -> node.path("id").asInt(), node -> node, (a, b) -> a));
        Map<Integer, JsonNode> originals = iterable(original.path("episodes")).stream()
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
        for (JsonNode episode : episodes) {
            int id = episode.path("id").asInt();
            int number = episode.path("episode_number").asInt();
            JsonNode englishEpisode = english.getOrDefault(id, episode);
            JsonNode originalEpisode = originals.getOrDefault(id, englishEpisode);
            String title = limit(firstText(episode, englishEpisode, originalEpisode, "name"), 255);
            if (title == null) {
                title = season.getSeasonNumber() == 0 ? "스페셜 " + number + "화" : number + "화";
            }
            if (id <= 0 || number < 0 || !knownEpisodeIds.add(id)) continue;
            episodeRepository.save(Episode.builder()
                .season(season).episodeNumber(number).title(title)
                .description(firstText(episode, englishEpisode, originalEpisode, "overview"))
                .thumbnailUrl(properties.imageUrl(firstText(
                    episode, englishEpisode, originalEpisode, "still_path")))
                .runtime(positiveInt(episode.path("runtime")))
                .externalSource(SOURCE).externalId(id).build());
        }
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
    private static String firstText(JsonNode primary, JsonNode fallback, JsonNode original, String field) {
        String value = firstText(primary, fallback, field);
        return value != null ? value : text(original, field, null);
    }
    private static String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("").strip();
        return value.isEmpty() ? fallback : value;
    }
    private static LocalDate date(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); } catch (RuntimeException ignored) { return null; }
    }
    private static boolean within(LocalDate value, LocalDate from, LocalDate to) {
        return value != null && !value.isBefore(from) && !value.isAfter(to);
    }
    private static boolean isImportTarget(SeasonData data, LocalDate from, LocalDate to) {
        int seasonNumber = data.candidate().path("season_number").asInt();
        if (seasonNumber > 0) {
            return !data.existing()
                && within(date(text(data.candidate(), "air_date", null)), from, to)
                && firstText(data.ko(), data.en(), data.original(), "name") != null
                && firstText(data.ko(), data.en(), data.original(), "overview") != null;
        }
        if (data.hidden()) return false;
        boolean hasRecentEpisode = iterable(data.ko().path("episodes")).stream()
            .anyMatch(episode -> within(date(text(episode, "air_date", null)), from, to));
        if (!hasRecentEpisode) return false;
        return data.existing()
            || (firstText(data.ko(), data.en(), data.original(), "name") != null
                && firstText(data.ko(), data.en(), data.original(), "overview") != null);
    }
    private boolean shouldLoadSeasonDetails(JsonNode candidate, LocalDate from, LocalDate to) {
        if (candidate.path("season_number").asInt() == 0) return true;
        if (!within(date(text(candidate, "air_date", null)), from, to)) return false;
        return contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE,
            ContentType.TV_SEASON,
            candidate.path("id").asInt()
        ).isEmpty();
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
        JsonNode original,
        boolean existing,
        boolean hidden
    ) { }
}
