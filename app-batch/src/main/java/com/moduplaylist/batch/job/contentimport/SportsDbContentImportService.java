package com.moduplaylist.batch.job.contentimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingService;
import com.moduplaylist.batch.job.contentimport.ContentImportMetrics.SportsRetryTarget;
import com.moduplaylist.batch.job.contentimport.SportsImportProperties.SportCode;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.entity.SportEvent.NormalizedStatus;
import com.moduplaylist.core.content.entity.SportType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import com.moduplaylist.core.content.repository.SportTypeRepository;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException.FailureType;
import com.moduplaylist.infrastructure.sportsdb.SportsDbClient;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteSynchronizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SportsDbContentImportService {
    private static final String SOURCE = "THESPORTSDB";
    private static final ZoneId CONTENT_IMPORT_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<NormalizedStatus> TRACKED_STATUSES = Set.of(
        NormalizedStatus.SCHEDULED,
        NormalizedStatus.LIVE
    );
    private static final int RECHECK_LIMIT = 50;
    private static final Map<String, NormalizedStatus> STATUS_MAPPING = Map.ofEntries(
        Map.entry("NS", NormalizedStatus.SCHEDULED),
        Map.entry("TBD", NormalizedStatus.SCHEDULED),
        Map.entry("Q1", NormalizedStatus.LIVE),
        Map.entry("Q2", NormalizedStatus.LIVE),
        Map.entry("Q3", NormalizedStatus.LIVE),
        Map.entry("Q4", NormalizedStatus.LIVE),
        Map.entry("IN1", NormalizedStatus.LIVE),
        Map.entry("IN2", NormalizedStatus.LIVE),
        Map.entry("IN3", NormalizedStatus.LIVE),
        Map.entry("IN4", NormalizedStatus.LIVE),
        Map.entry("IN5", NormalizedStatus.LIVE),
        Map.entry("IN6", NormalizedStatus.LIVE),
        Map.entry("IN7", NormalizedStatus.LIVE),
        Map.entry("IN8", NormalizedStatus.LIVE),
        Map.entry("IN9", NormalizedStatus.LIVE),
        Map.entry("OT", NormalizedStatus.LIVE),
        Map.entry("BT", NormalizedStatus.LIVE),
        Map.entry("HT", NormalizedStatus.LIVE),
        Map.entry("1H", NormalizedStatus.LIVE),
        Map.entry("2H", NormalizedStatus.LIVE),
        Map.entry("ET", NormalizedStatus.LIVE),
        Map.entry("P", NormalizedStatus.LIVE),
        Map.entry("S1", NormalizedStatus.LIVE),
        Map.entry("S2", NormalizedStatus.LIVE),
        Map.entry("S3", NormalizedStatus.LIVE),
        Map.entry("S4", NormalizedStatus.LIVE),
        Map.entry("S5", NormalizedStatus.LIVE),
        Map.entry("FT", NormalizedStatus.FINISHED),
        Map.entry("AOT", NormalizedStatus.FINISHED),
        Map.entry("AET", NormalizedStatus.FINISHED),
        Map.entry("PEN", NormalizedStatus.FINISHED),
        Map.entry("AWD", NormalizedStatus.FINISHED),
        Map.entry("AW", NormalizedStatus.FINISHED),
        Map.entry("WO", NormalizedStatus.FINISHED),
        Map.entry("PST", NormalizedStatus.POSTPONED),
        Map.entry("POST", NormalizedStatus.POSTPONED),
        Map.entry("CANC", NormalizedStatus.CANCELLED),
        Map.entry("ABD", NormalizedStatus.CANCELLED),
        Map.entry("SUSP", NormalizedStatus.SUSPENDED),
        Map.entry("INTR", NormalizedStatus.SUSPENDED),
        Map.entry("INT", NormalizedStatus.SUSPENDED)
    );

    private final SportsDbClient client;
    private final SportsImportProperties properties;
    private final ContentRepository contentRepository;
    private final SportEventRepository sportEventRepository;
    private final SportTypeRepository sportTypeRepository;
    private final ContentEmbeddingService contentEmbeddingService;
    private final ContentAutocompleteSynchronizer autocompleteSynchronizer;
    private final TransactionTemplate transactionTemplate;

    public void syncEvents(LocalDate runDate, ContentImportMetrics metrics) {
        Set<Integer> handled = new HashSet<>();
        Set<UUID> attemptedSearchContentIds = new HashSet<>();
        Set<UUID> attemptedAutocompleteContentIds = new HashSet<>();
        retryFailedSportImports(
            handled,
            attemptedSearchContentIds,
            attemptedAutocompleteContentIds,
            metrics
        );
        retryFailedSportSynchronizations(
            metrics,
            attemptedSearchContentIds,
            attemptedAutocompleteContentIds
        );
        Set<String> handledLeagueIds = new HashSet<>();
        int activeLeagueCount = 0;
        for (SportsImportProperties.League league : properties.getLeagues()) {
            if (!isEnabledLeague(league)) continue;
            if (!handledLeagueIds.add(league.getExternalLeagueId())) continue;
            if (++activeLeagueCount > 16) {
                log.warn("TheSportsDB 자동 수집 리그는 최대 16개만 처리합니다.");
                break;
            }
            for (int offset = -3; offset <= 7; offset++) {
                LocalDate date = runDate.plusDays(offset);
                JsonNode events = client.eventsOn(date, league.getExternalLeagueId());
                if (!events.isArray()) continue;
                if (events.size() == 3) {
                    metrics.freeLimitHit();
                    log.warn(
                        "TheSportsDB 무료 응답이 3건이어서 일정이 잘렸을 수 있습니다. leagueId={}, date={}",
                        league.getExternalLeagueId(), date
                    );
                }
                for (JsonNode event : events) {
                    if (!league.getExternalLeagueId().equals(text(event, "idLeague"))) continue;
                    if (!league.getSportCode().name().equals(externalSportCode(event))) continue;
                    Integer id = integer(event, "idEvent");
                    if (id != null && handled.add(id)) {
                        metrics.candidate();
                        SportsRetryTarget retryTarget = new SportsRetryTarget(
                            id,
                            league.getExternalLeagueId(),
                            league.getSportCode()
                        );
                        try {
                            SportSyncResult result = transactionTemplate.execute(
                                status -> syncEvent(event, id, league)
                            );
                            record(result, metrics);
                            synchronizePostProcessing(
                                result,
                                attemptedSearchContentIds,
                                attemptedAutocompleteContentIds,
                                metrics
                            );
                        } catch (RuntimeException exception) {
                            rethrowIfFatal(exception);
                            metrics.addSportsRetry(retryTarget);
                            metrics.failed("sport:" + id);
                            log.warn("TheSportsDB 경기 저장에 실패해 다음 경기를 처리합니다. externalEventId={}",
                                id, exception);
                        }
                    }
                }
            }
        }
        syncStoredEvents(
            runDate,
            handled,
            attemptedSearchContentIds,
            attemptedAutocompleteContentIds,
            metrics
        );
    }

    private void syncStoredEvents(
        LocalDate runDate,
        Set<Integer> handled,
        Set<UUID> attemptedSearchContentIds,
        Set<UUID> attemptedAutocompleteContentIds,
        ContentImportMetrics metrics
    ) {
        Instant from = runDate.minusDays(3).atStartOfDay(CONTENT_IMPORT_ZONE).toInstant();
        Instant to = runDate.plusDays(8).atStartOfDay(CONTENT_IMPORT_ZONE).toInstant();
        int fetchLimit = RECHECK_LIMIT + handled.size();
        int rechecked = 0;
        for (SportEvent candidate : sportEventRepository.findAllBatchUpdateCandidates(
            SOURCE, TRACKED_STATUSES, from, to, PageRequest.of(0, fetchLimit))) {
            Integer externalId = candidate.getContent().getExternalId();
            if (externalId == null || !handled.add(externalId)) continue;
            if (rechecked++ >= RECHECK_LIMIT) break;
            metrics.candidate();
            SportsRetryTarget retryTarget = sportsRetryTarget(candidate);

            try {
                JsonNode event = client.event(externalId);
                if (!event.isObject()) {
                    if (retryTarget != null) metrics.addSportsRetry(retryTarget);
                    metrics.failed("sport:" + externalId);
                    log.warn("TheSportsDB 기존 경기 조회 결과가 없습니다. externalEventId={}", externalId);
                    continue;
                }

                SportSyncResult result = transactionTemplate.execute(status ->
                    sportEventRepository.findById(candidate.getContentId())
                        .filter(existing -> !existing.getContent().isHidden())
                        .map(existing -> update(existing, event, externalId))
                        .orElseGet(() -> SportSyncResult.existingResult(null))
                );
                record(result, metrics);
                synchronizePostProcessing(
                    result,
                    attemptedSearchContentIds,
                    attemptedAutocompleteContentIds,
                    metrics
                );
            } catch (RuntimeException exception) {
                rethrowIfFatal(exception);
                if (retryTarget != null) metrics.addSportsRetry(retryTarget);
                metrics.failed("sport:" + externalId);
                log.warn("TheSportsDB 기존 경기 갱신에 실패해 다음 경기를 처리합니다. externalEventId={}",
                    externalId, exception);
            }
        }
    }

    private static void record(SportSyncResult result, ContentImportMetrics metrics) {
        if (result.existing()) metrics.existing();
        if (result.created()) metrics.created();
        if (result.updated()) metrics.sportUpdated();
        if (result.missingRequired()) metrics.missingRequired();
    }

    private void synchronizePostProcessing(
        SportSyncResult result,
        Set<UUID> attemptedSearchContentIds,
        Set<UUID> attemptedAutocompleteContentIds,
        ContentImportMetrics metrics
    ) {
        if (result == null || result.contentId() == null) return;
        UUID contentId = result.contentId();
        if (result.searchDocumentSyncRequired() && attemptedSearchContentIds.add(contentId)) {
            try {
                synchronizeSportSearchDocumentSafely(contentId, metrics);
            } catch (RuntimeException exception) {
                if (result.autocompleteSyncRequired()) {
                    metrics.addSportsAutocompleteRetry(contentId);
                }
                throw exception;
            }
        }
        if (result.autocompleteSyncRequired()
            && attemptedAutocompleteContentIds.add(contentId)) {
            synchronizeSportAutocompleteSafely(contentId, metrics);
        }
    }

    private void synchronizeSportSearchDocumentSafely(
        UUID contentId,
        ContentImportMetrics metrics
    ) {
        try {
            contentEmbeddingService.indexSportSearchDocument(contentId);
            metrics.completeSportsSearchRetry(contentId);
        } catch (RuntimeException exception) {
            metrics.addSportsSearchRetry(contentId);
            rethrowIfFatal(exception);
            metrics.failed("sport-search:" + contentId);
            log.warn("TheSportsDB 검색 문서 동기화에 실패했습니다. contentId={}",
                contentId, exception);
        }
    }

    private void synchronizeSportAutocompleteSafely(
        UUID contentId,
        ContentImportMetrics metrics
    ) {
        try {
            autocompleteSynchronizer.synchronize(contentId);
            metrics.completeSportsAutocompleteRetry(contentId);
        } catch (RuntimeException exception) {
            metrics.addSportsAutocompleteRetry(contentId);
            rethrowIfFatal(exception);
            metrics.failed("sport-autocomplete:" + contentId);
            log.warn("TheSportsDB 자동완성 동기화에 실패했습니다. contentId={}",
                contentId, exception);
        }
    }

    private void retryFailedSportSynchronizations(
        ContentImportMetrics metrics,
        Set<UUID> attemptedSearchContentIds,
        Set<UUID> attemptedAutocompleteContentIds
    ) {
        for (UUID contentId : metrics.sportsSearchRetryContentIds()) {
            attemptedSearchContentIds.add(contentId);
            synchronizeSportSearchDocumentSafely(contentId, metrics);
        }
        for (UUID contentId : metrics.sportsAutocompleteRetryContentIds()) {
            attemptedAutocompleteContentIds.add(contentId);
            synchronizeSportAutocompleteSafely(contentId, metrics);
        }
    }

    private void retryFailedSportImports(
        Set<Integer> handled,
        Set<UUID> attemptedSearchContentIds,
        Set<UUID> attemptedAutocompleteContentIds,
        ContentImportMetrics metrics
    ) {
        for (SportsRetryTarget target : metrics.sportsRetryTargets()) {
            if (!handled.add(target.eventId())) continue;
            try {
                JsonNode event = client.event(target.eventId());
                if (!event.isObject()) {
                    metrics.failed("sport:" + target.eventId());
                    log.warn(
                        "TheSportsDB 재시도 경기 조회 결과가 없습니다. externalEventId={}",
                        target.eventId()
                    );
                    continue;
                }
                SportSyncResult result = transactionTemplate.execute(status ->
                    syncEvent(event, target.eventId(), league(target))
                );
                record(result, metrics);
                synchronizePostProcessing(
                    result,
                    attemptedSearchContentIds,
                    attemptedAutocompleteContentIds,
                    metrics
                );
                metrics.completeSportsRetry(target);
            } catch (RuntimeException exception) {
                rethrowIfFatal(exception);
                metrics.failed("sport:" + target.eventId());
                log.warn(
                    "TheSportsDB 경기 재처리에 실패했습니다. externalEventId={}",
                    target.eventId(),
                    exception
                );
            }
        }
    }

    private static SportsImportProperties.League league(SportsRetryTarget target) {
        SportsImportProperties.League league = new SportsImportProperties.League();
        league.setExternalLeagueId(target.externalLeagueId());
        league.setSportCode(target.sportCode());
        return league;
    }

    private static SportsRetryTarget sportsRetryTarget(SportEvent event) {
        try {
            return new SportsRetryTarget(
                event.getContent().getExternalId(),
                event.getExternalLeagueId(),
                SportCode.valueOf(event.getSportType().getCode())
            );
        } catch (IllegalArgumentException exception) {
            return null;
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

    private boolean isEnabledLeague(SportsImportProperties.League league) {
        if (!league.isEnabled()) return false;
        if (league.getSportCode() == null) {
            log.warn("종목 코드가 없는 리그를 건너뜁니다. leagueId={}", league.getExternalLeagueId());
            return false;
        }
        return league.getExternalLeagueId() != null && !league.getExternalLeagueId().isBlank();
    }

    private SportSyncResult syncEvent(JsonNode event, int id, SportsImportProperties.League league) {
        validateExternalEventId(id);
        Content existingContent = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.SPORT, id).orElse(null);
        if (existingContent != null) {
            if (existingContent.isHidden()) return SportSyncResult.existingResult(null);
            return sportEventRepository.findById(existingContent.getId())
                .map(existing -> update(existing, event, id))
                .orElseThrow(() -> new IllegalStateException(
                    "외부 스포츠 콘텐츠에 경기 정보가 없습니다. contentId="
                        + existingContent.getId() + ", externalEventId=" + id
                ));
        }
        return importEvent(event, id, league);
    }

    private SportSyncResult importEvent(JsonNode event, int id, SportsImportProperties.League league) {
        String title = limit(text(event, "strEvent"), 255);
        String home = limit(text(event, "strHomeTeam"), 255);
        String away = limit(text(event, "strAwayTeam"), 255);
        String sportName = text(event, "strSport");
        if (title == null || home == null || away == null || sportName == null) {
            return SportSyncResult.missingRequiredResult();
        }
        Instant scheduledAt = scheduledAt(event);
        SportType sportType = sportType(league.getSportCode(), sportName);
        Integer homeScore = integer(event, "intHomeScore");
        Integer awayScore = integer(event, "intAwayScore");
        validateExternalScores(id, homeScore, awayScore);
        if ((homeScore == null) != (awayScore == null)) {
            homeScore = null;
            awayScore = null;
        }
        Content content = contentRepository.save(Content.builder()
            .title(title).type(ContentType.SPORT).description(text(event, "strDescriptionEN"))
            .thumbnailUrl(limit(firstText(event, "strThumb", "strPoster", "strFanart"), 500))
            .externalSource(SOURCE).externalId(id).build());
        sportEventRepository.save(SportEvent.create(
            content, sportType, limit(league.getExternalLeagueId(), 100), limit(text(event, "strLeague"), 255),
            limit(text(event, "strSeason"), 100), limit(text(event, "intRound"), 100), home, away,
            limit(text(event, "strVenue"), 255), limit(text(event, "strCountry"), 100), scheduledAt,
            homeScore, awayScore, limit(rawStatus(event), 100), normalizedStatus(event), Instant.now()
        ));
        return SportSyncResult.createdResult(content.getId());
    }

    private SportSyncResult update(SportEvent existing, JsonNode event, int externalEventId) {
        validateExternalEventId(externalEventId);
        String title = limit(text(event, "strEvent"), 255);
        String home = limit(text(event, "strHomeTeam"), 255);
        String away = limit(text(event, "strAwayTeam"), 255);
        if (title == null || home == null || away == null) {
            existing.updateStatus(
                existing.getRawStatus(),
                existing.getNormalizedStatus(),
                Instant.now()
            );
            return SportSyncResult.existingWithMissingRequired(existing.getContentId());
        }
        Instant scheduledAt = scheduledAt(event);
        Integer homeScore = integer(event, "intHomeScore");
        Integer awayScore = integer(event, "intAwayScore");
        validateExternalScores(externalEventId, homeScore, awayScore);
        if (homeScore == null || awayScore == null) {
            homeScore = existing.getHomeScore();
            awayScore = existing.getAwayScore();
        }
        String description = text(event, "strDescriptionEN");
        String thumbnailUrl = limit(firstText(event, "strThumb", "strPoster", "strFanart"), 500);
        String leagueName = limit(text(event, "strLeague"), 255);
        String season = limit(text(event, "strSeason"), 100);
        String round = limit(text(event, "intRound"), 100);
        String venue = limit(text(event, "strVenue"), 255);
        String country = limit(text(event, "strCountry"), 100);
        String rawStatus = limit(rawStatus(event), 100);
        NormalizedStatus normalizedStatus = normalizedStatus(event);
        boolean commonSearchChanged = !Objects.equals(existing.getContent().getTitle(), title)
            || !Objects.equals(existing.getLeagueName(), leagueName)
            || !Objects.equals(existing.getSeason(), season)
            || !Objects.equals(existing.getHomeTeamName(), home)
            || !Objects.equals(existing.getAwayTeamName(), away);
        boolean descriptionChanged = !Objects.equals(
            existing.getContent().getDescription(), description);
        boolean searchDocumentChanged = commonSearchChanged || descriptionChanged;
        boolean changed = searchDocumentChanged
            || !Objects.equals(existing.getContent().getThumbnailUrl(), thumbnailUrl)
            || !Objects.equals(existing.getRound(), round)
            || !Objects.equals(existing.getVenue(), venue)
            || !Objects.equals(existing.getCountry(), country)
            || !Objects.equals(existing.getScheduledAt(), scheduledAt)
            || !Objects.equals(existing.getHomeScore(), homeScore)
            || !Objects.equals(existing.getAwayScore(), awayScore)
            || !Objects.equals(existing.getRawStatus(), rawStatus)
            || existing.getNormalizedStatus() != normalizedStatus;

        if (!changed) {
            existing.updateStatus(existing.getRawStatus(), existing.getNormalizedStatus(), Instant.now());
            return SportSyncResult.existingResult(existing.getContentId());
        }
        existing.getContent().updateCommonDetails(title, text(event, "strDescriptionEN"), null, null);
        existing.getContent().replaceThumbnailUrl(thumbnailUrl);
        existing.updateDetails(
            existing.getSportType(), leagueName, season, round, home, away, venue, country,
            scheduledAt, homeScore, awayScore
        );
        existing.updateStatus(rawStatus, normalizedStatus, Instant.now());
        existing.getContent().markUpdated();
        return SportSyncResult.updatedResult(
            existing.getContentId(),
            searchDocumentChanged,
            commonSearchChanged
        );
    }

    private static void validateExternalEventId(int eventId) {
        if (eventId <= 0) {
            throw new ExternalApiException(
                FailureType.ITEM_FAILURE,
                "TheSportsDB 경기 ID가 유효하지 않습니다. eventId=" + eventId
            );
        }
    }

    private static void validateExternalScores(
        int eventId,
        Integer homeScore,
        Integer awayScore
    ) {
        if (homeScore != null && homeScore < 0
            || awayScore != null && awayScore < 0) {
            throw new ExternalApiException(
                FailureType.ITEM_FAILURE,
                "TheSportsDB 경기 점수가 유효하지 않습니다. eventId=" + eventId
            );
        }
    }

    private SportType sportType(SportCode configuredCode, String name) {
        String normalizedName = limit(name, 100);
        String code = configuredCode.name();
        return sportTypeRepository.findByCode(code)
            .orElseGet(() -> sportTypeRepository.save(SportType.create(code, normalizedName)));
    }

    private static String normalizedCode(String value) {
        if (value == null) return "";
        return value.strip().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    private static String externalSportCode(JsonNode event) {
        String value = text(event, "strSport");
        if (value == null) return "";
        if (value.equalsIgnoreCase("Soccer")) return "SOCCER";
        return normalizedCode(value);
    }

    private static Instant scheduledAt(JsonNode event) {
        String timestamp = text(event, "strTimestamp");
        if (timestamp != null) {
            try { return Instant.parse(timestamp); } catch (DateTimeParseException ignored) { }
            try {
                return LocalDateTime.parse(timestamp).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) { }
        }
        String date = text(event, "dateEvent");
        if (date == null) return null;
        String time = text(event, "strTime");
        try {
            return LocalDateTime.parse(date + "T" + (time == null ? "00:00:00" : normalizeTime(time)))
                .toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String normalizeTime(String time) {
        return time.length() == 5 ? time + ":00" : time;
    }

    private static String rawStatus(JsonNode event) {
        String status = text(event, "strStatus");
        return status != null ? status : text(event, "strProgress");
    }

    private static NormalizedStatus normalizedStatus(JsonNode event) {
        String status = rawStatus(event);
        if (status == null) {
            boolean hasScore = integer(event, "intHomeScore") != null
                || integer(event, "intAwayScore") != null;
            return hasScore ? NormalizedStatus.UNKNOWN : NormalizedStatus.SCHEDULED;
        }
        return STATUS_MAPPING.getOrDefault(
            status.strip().toUpperCase(Locale.ROOT),
            NormalizedStatus.UNKNOWN
        );
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText("").strip();
        return value.isEmpty() || value.equalsIgnoreCase("null") ? null : value;
    }

    private static Integer integer(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; }
    }

    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record SportSyncResult(
        UUID contentId,
        boolean existing,
        boolean created,
        boolean updated,
        boolean missingRequired,
        boolean searchDocumentSyncRequired,
        boolean autocompleteSyncRequired
    ) {
        private static SportSyncResult existingResult(UUID contentId) {
            return new SportSyncResult(
                contentId, true, false, false, false, false, false);
        }

        private static SportSyncResult createdResult(UUID contentId) {
            return new SportSyncResult(
                contentId, false, true, false, false, true, true);
        }

        private static SportSyncResult updatedResult(
            UUID contentId,
            boolean searchDocumentSyncRequired,
            boolean autocompleteSyncRequired
        ) {
            return new SportSyncResult(
                contentId,
                true,
                false,
                true,
                false,
                searchDocumentSyncRequired,
                autocompleteSyncRequired
            );
        }

        private static SportSyncResult missingRequiredResult() {
            return new SportSyncResult(
                null, false, false, false, true, false, false);
        }

        private static SportSyncResult existingWithMissingRequired(UUID contentId) {
            return new SportSyncResult(
                contentId, true, false, false, true, false, false);
        }
    }
}
