package com.moduplaylist.batch.job.contentimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.entity.SportEvent.NormalizedStatus;
import com.moduplaylist.core.content.entity.SportType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import com.moduplaylist.core.content.repository.SportTypeRepository;
import com.moduplaylist.infrastructure.sportsdb.SportsDbClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SportsDbContentImportService {
    private static final String SOURCE = "THESPORTSDB";
    private static final Set<String> AUTOMATIC_SPORT_CODES = Set.of(
        "SOCCER", "BASKETBALL", "BASEBALL", "VOLLEYBALL"
    );

    private final SportsDbClient client;
    private final SportsImportProperties properties;
    private final ContentRepository contentRepository;
    private final SportEventRepository sportEventRepository;
    private final SportTypeRepository sportTypeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public void syncEvents(LocalDate runDate) {
        Set<Integer> handled = new HashSet<>();
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
                if (events.size() >= 3) {
                    log.warn(
                        "TheSportsDB 무료 응답이 3건이어서 일정이 잘렸을 수 있습니다. leagueId={}, date={}",
                        league.getExternalLeagueId(), date
                    );
                }
                for (JsonNode event : events) {
                    if (!league.getExternalLeagueId().equals(text(event, "idLeague"))) continue;
                    if (!normalizedCode(league.getSportCode()).equals(externalSportCode(event))) continue;
                    Integer id = integer(event, "idEvent");
                    if (id != null && handled.add(id)) {
                        transactionTemplate.executeWithoutResult(status -> syncEvent(event, id, league));
                    }
                }
            }
        }
    }

    private boolean isEnabledLeague(SportsImportProperties.League league) {
        if (!league.isEnabled()) return false;
        String sportCode = normalizedCode(league.getSportCode());
        if (!AUTOMATIC_SPORT_CODES.contains(sportCode)) {
            log.warn("자동 수집 대상이 아닌 종목을 건너뜁니다. sportCode={}, leagueId={}",
                league.getSportCode(), league.getExternalLeagueId());
            return false;
        }
        return league.getExternalLeagueId() != null && !league.getExternalLeagueId().isBlank();
    }

    private void syncEvent(JsonNode event, int id, SportsImportProperties.League league) {
        Content existingContent = contentRepository.findByExternalSourceAndTypeAndExternalId(
            SOURCE, ContentType.SPORT, id).orElse(null);
        if (existingContent != null) {
            if (existingContent.isHidden()) return;
            sportEventRepository.findById(existingContent.getId())
                .ifPresent(existing -> update(existing, event));
            return;
        }
        importEvent(event, id, league);
    }

    private void importEvent(JsonNode event, int id, SportsImportProperties.League league) {
        String title = limit(text(event, "strEvent"), 255);
        String home = limit(text(event, "strHomeTeam"), 255);
        String away = limit(text(event, "strAwayTeam"), 255);
        String sportName = text(event, "strSport");
        if (title == null || home == null || away == null || sportName == null) return;
        Instant scheduledAt = scheduledAt(event);
        if (sportEventRepository.existsDuplicate(title, home, away, scheduledAt)) return;
        SportType sportType = sportType(league.getSportCode(), sportName);
        Integer homeScore = integer(event, "intHomeScore");
        Integer awayScore = integer(event, "intAwayScore");
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
        eventPublisher.publishEvent(new SportSearchSyncRequested(content.getId()));
    }

    private void update(SportEvent existing, JsonNode event) {
        String title = limit(text(event, "strEvent"), 255);
        String home = limit(text(event, "strHomeTeam"), 255);
        String away = limit(text(event, "strAwayTeam"), 255);
        if (title == null || home == null || away == null) return;
        Instant scheduledAt = scheduledAt(event);
        if (sportEventRepository.existsDuplicateExcluding(existing.getContentId(), title, home, away, scheduledAt)) return;
        Integer homeScore = integer(event, "intHomeScore");
        Integer awayScore = integer(event, "intAwayScore");
        if ((homeScore == null) != (awayScore == null)) {
            homeScore = null;
            awayScore = null;
        }
        String description = text(event, "strDescriptionEN");
        String thumbnailUrl = limit(firstText(event, "strThumb", "strPoster", "strFanart"), 500);
        String season = limit(text(event, "strSeason"), 100);
        String round = limit(text(event, "intRound"), 100);
        String venue = limit(text(event, "strVenue"), 255);
        String country = limit(text(event, "strCountry"), 100);
        String rawStatus = limit(rawStatus(event), 100);
        NormalizedStatus normalizedStatus = normalizedStatus(event);
        boolean changed = !Objects.equals(existing.getContent().getTitle(), title)
            || !Objects.equals(existing.getContent().getDescription(), description)
            || !Objects.equals(existing.getContent().getThumbnailUrl(), thumbnailUrl)
            || !Objects.equals(existing.getSeason(), season)
            || !Objects.equals(existing.getRound(), round)
            || !Objects.equals(existing.getHomeTeamName(), home)
            || !Objects.equals(existing.getAwayTeamName(), away)
            || !Objects.equals(existing.getVenue(), venue)
            || !Objects.equals(existing.getCountry(), country)
            || !Objects.equals(existing.getScheduledAt(), scheduledAt)
            || !Objects.equals(existing.getHomeScore(), homeScore)
            || !Objects.equals(existing.getAwayScore(), awayScore)
            || !Objects.equals(existing.getRawStatus(), rawStatus)
            || existing.getNormalizedStatus() != normalizedStatus;

        if (!changed) {
            existing.updateStatus(existing.getRawStatus(), existing.getNormalizedStatus(), Instant.now());
            return;
        }
        existing.getContent().updateCommonDetails(title, text(event, "strDescriptionEN"), null, null);
        existing.getContent().replaceThumbnailUrl(thumbnailUrl);
        existing.updateMutableDetails(
            season, round, home, away, venue, country, scheduledAt, homeScore, awayScore
        );
        existing.updateStatus(rawStatus, normalizedStatus, Instant.now());
        existing.getContent().markUpdated();
        eventPublisher.publishEvent(new SportSearchSyncRequested(existing.getContentId()));
    }

    private SportType sportType(String configuredCode, String name) {
        name = limit(name, 100);
        String code = normalizedCode(configuredCode);
        if (code.length() > 50) code = code.substring(0, 50);
        String finalCode = code;
        return sportTypeRepository.findByCode(finalCode)
            .orElseGet(() -> sportTypeRepository.save(SportType.create(finalCode, name)));
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
            return integer(event, "intHomeScore") == null ? NormalizedStatus.SCHEDULED : NormalizedStatus.FINISHED;
        }
        String value = status.toLowerCase(Locale.ROOT);
        if (value.contains("postpon")) return NormalizedStatus.POSTPONED;
        if (value.contains("cancel") || value.contains("aband")) return NormalizedStatus.CANCELLED;
        if (value.contains("suspend") || value.contains("interrupt")) return NormalizedStatus.SUSPENDED;
        if (value.equals("ft") || value.contains("finish") || value.contains("after")) return NormalizedStatus.FINISHED;
        if (value.contains("live") || value.matches(".*\\b[0-9]{1,3}'?\\b.*") || value.equals("ht")) return NormalizedStatus.LIVE;
        if (value.contains("not started") || value.contains("schedule") || value.equals("ns")) return NormalizedStatus.SCHEDULED;
        return NormalizedStatus.UNKNOWN;
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
}
