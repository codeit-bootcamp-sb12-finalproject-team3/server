package com.moduplaylist.batch.job.contentimport;

import com.moduplaylist.batch.job.contentimport.SportsImportProperties.SportCode;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.batch.item.ExecutionContext;

public class ContentImportMetrics {
    private static final int FAILED_ID_LIMIT = 20;
    static final String CANDIDATE_COUNT = "candidateCount";
    static final String EXISTING_COUNT = "existingCount";
    static final String CREATED_COUNT = "createdCount";
    static final String SPORT_UPDATED_COUNT = "sportUpdatedCount";
    static final String MISSING_REQUIRED_COUNT = "missingRequiredCount";
    static final String FREE_LIMIT_HIT_COUNT = "freeLimitHitCount";
    static final String FAILED_COUNT = "failedCount";
    private static final String TMDB_PROVIDER_RETRY_TARGETS = "tmdbProviderRetryTargets";
    private static final String TMDB_MOVIE_PROVIDER_RETRY_TARGETS =
        "tmdbMovieProviderRetryTargets";
    private static final String AUTOCOMPLETE_RETRY_CONTENT_IDS = "autocompleteRetryContentIds";
    private static final String SPORTS_SEARCH_RETRY_CONTENT_IDS = "sportsSearchRetryContentIds";
    private static final String SPORTS_AUTOCOMPLETE_RETRY_CONTENT_IDS =
        "sportsAutocompleteRetryContentIds";
    private static final String MOVIE_RETRY_IDS = "movieRetryIds";
    private static final String TV_SERIES_RETRY_IDS = "tvSeriesRetryIds";
    private static final String SPORTS_RETRY_TARGETS = "sportsRetryTargets";

    private long candidateCount;
    private long existingCount;
    private long createdCount;
    private long sportUpdatedCount;
    private long missingRequiredCount;
    private long freeLimitHitCount;
    private final Set<String> failedIds = new LinkedHashSet<>();
    private final Set<String> recordedFailedIds = new LinkedHashSet<>();
    private final Set<TmdbProviderRetryTarget> tmdbProviderRetryTargets = new LinkedHashSet<>();
    private final Set<TmdbMovieProviderRetryTarget> tmdbMovieProviderRetryTargets =
        new LinkedHashSet<>();
    private final Set<UUID> autocompleteRetryContentIds = new LinkedHashSet<>();
    private final Set<UUID> sportsSearchRetryContentIds = new LinkedHashSet<>();
    private final Set<UUID> sportsAutocompleteRetryContentIds = new LinkedHashSet<>();
    private final Set<Integer> movieRetryIds = new LinkedHashSet<>();
    private final Set<Integer> tvSeriesRetryIds = new LinkedHashSet<>();
    private final Set<SportsRetryTarget> sportsRetryTargets = new LinkedHashSet<>();

    public ContentImportMetrics() {
    }

    public ContentImportMetrics(ExecutionContext context) {
        restoreTmdbProviderRetryTargets(context);
        restoreTmdbMovieProviderRetryTargets(context);
        restoreAutocompleteRetryContentIds(context);
        restoreSportsSearchRetryContentIds(context);
        restoreSportsAutocompleteRetryContentIds(context);
        restoreMovieRetryIds(context);
        restoreTvSeriesRetryIds(context);
        restoreSportsRetryTargets(context);
    }

    public void candidate() {
        candidateCount++;
    }

    public void existing() {
        existingCount++;
    }

    public void created() {
        createdCount++;
    }

    public void created(long count) {
        createdCount += count;
    }

    public void sportUpdated() {
        sportUpdatedCount++;
    }

    public void missingRequired() {
        missingRequiredCount++;
    }

    public void freeLimitHit() {
        freeLimitHitCount++;
    }

    public void failed(String externalId) {
        if (!failedIds.add(externalId)) return;
        if (recordedFailedIds.size() < FAILED_ID_LIMIT) {
            recordedFailedIds.add(externalId);
        }
    }

    public boolean hasFailures() {
        return !failedIds.isEmpty();
    }

    public Set<TmdbProviderRetryTarget> tmdbProviderRetryTargets() {
        return Set.copyOf(tmdbProviderRetryTargets);
    }

    public void addTmdbProviderRetry(TmdbProviderRetryTarget target) {
        tmdbProviderRetryTargets.add(target);
    }

    public void completeTmdbProviderRetry(TmdbProviderRetryTarget target) {
        tmdbProviderRetryTargets.remove(target);
    }

    public Set<TmdbMovieProviderRetryTarget> tmdbMovieProviderRetryTargets() {
        return Set.copyOf(tmdbMovieProviderRetryTargets);
    }

    public void addTmdbMovieProviderRetry(TmdbMovieProviderRetryTarget target) {
        tmdbMovieProviderRetryTargets.add(target);
    }

    public void completeTmdbMovieProviderRetry(TmdbMovieProviderRetryTarget target) {
        tmdbMovieProviderRetryTargets.remove(target);
    }

    public Set<UUID> autocompleteRetryContentIds() {
        return Set.copyOf(autocompleteRetryContentIds);
    }

    public void addAutocompleteRetry(UUID contentId) {
        autocompleteRetryContentIds.add(contentId);
    }

    public void completeAutocompleteRetry(UUID contentId) {
        autocompleteRetryContentIds.remove(contentId);
    }

    public Set<UUID> sportsSearchRetryContentIds() {
        return Set.copyOf(sportsSearchRetryContentIds);
    }

    public void addSportsSearchRetry(UUID contentId) {
        sportsSearchRetryContentIds.add(contentId);
    }

    public void completeSportsSearchRetry(UUID contentId) {
        sportsSearchRetryContentIds.remove(contentId);
    }

    public Set<UUID> sportsAutocompleteRetryContentIds() {
        return Set.copyOf(sportsAutocompleteRetryContentIds);
    }

    public void addSportsAutocompleteRetry(UUID contentId) {
        sportsAutocompleteRetryContentIds.add(contentId);
    }

    public void completeSportsAutocompleteRetry(UUID contentId) {
        sportsAutocompleteRetryContentIds.remove(contentId);
    }

    public Set<Integer> movieRetryIds() {
        return Set.copyOf(movieRetryIds);
    }

    public void addMovieRetry(int movieId) {
        movieRetryIds.add(movieId);
    }

    public void completeMovieRetry(int movieId) {
        movieRetryIds.remove(movieId);
    }

    public Set<Integer> tvSeriesRetryIds() {
        return Set.copyOf(tvSeriesRetryIds);
    }

    public void addTvSeriesRetry(int seriesId) {
        tvSeriesRetryIds.add(seriesId);
    }

    public void completeTvSeriesRetry(int seriesId) {
        tvSeriesRetryIds.remove(seriesId);
    }

    public Set<SportsRetryTarget> sportsRetryTargets() {
        return Set.copyOf(sportsRetryTargets);
    }

    public void addSportsRetry(SportsRetryTarget target) {
        sportsRetryTargets.add(target);
    }

    public void completeSportsRetry(SportsRetryTarget target) {
        sportsRetryTargets.remove(target);
    }

    public void writeTo(ExecutionContext context) {
        context.putLong(CANDIDATE_COUNT, candidateCount);
        context.putLong(EXISTING_COUNT, existingCount);
        context.putLong(CREATED_COUNT, createdCount);
        context.putLong(SPORT_UPDATED_COUNT, sportUpdatedCount);
        context.putLong(MISSING_REQUIRED_COUNT, missingRequiredCount);
        context.putLong(FREE_LIMIT_HIT_COUNT, freeLimitHitCount);
        context.putLong(FAILED_COUNT, failedIds.size());
        context.putString("failedIds", String.join(",", recordedFailedIds));
        context.putLong("failedIdsOmittedCount", failedIds.size() - recordedFailedIds.size());
        context.putString(TMDB_PROVIDER_RETRY_TARGETS, tmdbProviderRetryTargets.stream()
            .map(TmdbProviderRetryTarget::serialize)
            .collect(Collectors.joining(",")));
        context.putString(TMDB_MOVIE_PROVIDER_RETRY_TARGETS,
            tmdbMovieProviderRetryTargets.stream()
                .map(TmdbMovieProviderRetryTarget::serialize)
                .collect(Collectors.joining(",")));
        context.putString(AUTOCOMPLETE_RETRY_CONTENT_IDS, autocompleteRetryContentIds.stream()
            .map(UUID::toString)
            .collect(Collectors.joining(",")));
        context.putString(SPORTS_SEARCH_RETRY_CONTENT_IDS, sportsSearchRetryContentIds.stream()
            .map(UUID::toString)
            .collect(Collectors.joining(",")));
        context.putString(SPORTS_AUTOCOMPLETE_RETRY_CONTENT_IDS,
            sportsAutocompleteRetryContentIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(",")));
        context.putString(MOVIE_RETRY_IDS, movieRetryIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")));
        context.putString(TV_SERIES_RETRY_IDS, tvSeriesRetryIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")));
        context.putString(SPORTS_RETRY_TARGETS, sportsRetryTargets.stream()
            .map(SportsRetryTarget::serialize)
            .collect(Collectors.joining(",")));
    }

    public String summary() {
        return "candidates=" + candidateCount
            + ", existing=" + existingCount
            + ", created=" + createdCount
            + ", updated=" + sportUpdatedCount
            + ", missing=" + missingRequiredCount
            + ", limitHits=" + freeLimitHitCount
            + ", failed=" + failedIds.size()
            + ", failedIds=[" + String.join(",", recordedFailedIds) + "]"
            + ", omittedFailedIds=" + (failedIds.size() - recordedFailedIds.size());
    }

    private void restoreTmdbProviderRetryTargets(ExecutionContext context) {
        String serialized = context.getString(TMDB_PROVIDER_RETRY_TARGETS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            tmdbProviderRetryTargets.add(TmdbProviderRetryTarget.parse(value));
        }
    }

    private void restoreAutocompleteRetryContentIds(ExecutionContext context) {
        String serialized = context.getString(AUTOCOMPLETE_RETRY_CONTENT_IDS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            autocompleteRetryContentIds.add(UUID.fromString(value));
        }
    }

    private void restoreTmdbMovieProviderRetryTargets(ExecutionContext context) {
        String serialized = context.getString(TMDB_MOVIE_PROVIDER_RETRY_TARGETS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            tmdbMovieProviderRetryTargets.add(TmdbMovieProviderRetryTarget.parse(value));
        }
    }

    private void restoreSportsSearchRetryContentIds(ExecutionContext context) {
        String serialized = context.getString(SPORTS_SEARCH_RETRY_CONTENT_IDS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            sportsSearchRetryContentIds.add(UUID.fromString(value));
        }
    }

    private void restoreSportsAutocompleteRetryContentIds(ExecutionContext context) {
        String serialized = context.getString(SPORTS_AUTOCOMPLETE_RETRY_CONTENT_IDS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            sportsAutocompleteRetryContentIds.add(UUID.fromString(value));
        }
    }

    private void restoreMovieRetryIds(ExecutionContext context) {
        String serialized = context.getString(MOVIE_RETRY_IDS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            movieRetryIds.add(Integer.parseInt(value));
        }
    }

    private void restoreTvSeriesRetryIds(ExecutionContext context) {
        String serialized = context.getString(TV_SERIES_RETRY_IDS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            tvSeriesRetryIds.add(Integer.parseInt(value));
        }
    }

    private void restoreSportsRetryTargets(ExecutionContext context) {
        String serialized = context.getString(SPORTS_RETRY_TARGETS, "");
        if (serialized.isBlank()) return;
        for (String value : serialized.split(",")) {
            sportsRetryTargets.add(SportsRetryTarget.parse(value));
        }
    }

    public record TmdbMovieProviderRetryTarget(
        UUID contentId,
        int movieId
    ) {
        private String serialize() {
            return contentId + "|" + movieId;
        }

        private static TmdbMovieProviderRetryTarget parse(String value) {
            String[] parts = value.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalStateException("잘못된 TMDB 영화 제공처 재시도 대상입니다: " + value);
            }
            return new TmdbMovieProviderRetryTarget(
                UUID.fromString(parts[0]),
                Integer.parseInt(parts[1])
            );
        }
    }

    public record TmdbProviderRetryTarget(
        UUID contentId,
        int seriesId,
        int seasonNumber
    ) {
        private String serialize() {
            return contentId + "|" + seriesId + "|" + seasonNumber;
        }

        private static TmdbProviderRetryTarget parse(String value) {
            String[] parts = value.split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalStateException("잘못된 TMDB 제공처 재시도 대상입니다: " + value);
            }
            return new TmdbProviderRetryTarget(
                UUID.fromString(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        }
    }

    public record SportsRetryTarget(
        int eventId,
        String externalLeagueId,
        SportCode sportCode
    ) {
        private String serialize() {
            return eventId + "|" + externalLeagueId + "|" + sportCode.name();
        }

        private static SportsRetryTarget parse(String value) {
            String[] parts = value.split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalStateException("잘못된 스포츠 재시도 대상입니다: " + value);
            }
            return new SportsRetryTarget(
                Integer.parseInt(parts[0]),
                parts[1],
                SportCode.valueOf(parts[2])
            );
        }
    }
}
