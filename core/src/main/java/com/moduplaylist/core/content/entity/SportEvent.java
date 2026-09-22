package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "sport_events",
	indexes = {
		@Index(
			name = "idx_sport_events_type_schedule",
			columnList = "sport_type_id, scheduled_at, content_id"
		),
		@Index(
			name = "idx_sport_events_status_schedule",
			columnList = "normalized_status, scheduled_at, content_id"
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SportEvent {

	private static final int MAX_EXTERNAL_LEAGUE_ID_LENGTH = 100;
	private static final int MAX_LEAGUE_NAME_LENGTH = 255;
	private static final int MAX_SEASON_LENGTH = 100;
	private static final int MAX_ROUND_LENGTH = 100;
	private static final int MAX_TEAM_NAME_LENGTH = 255;
	private static final int MAX_VENUE_LENGTH = 255;
	private static final int MAX_COUNTRY_LENGTH = 100;
	private static final int MAX_RAW_STATUS_LENGTH = 100;

	@Id
	@Column(name = "content_id", nullable = false, updatable = false)
	private UUID contentId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "content_id", nullable = false, updatable = false)
	private Content content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sport_type_id", nullable = false)
	private SportType sportType;

	@Column(name = "external_league_id", length = MAX_EXTERNAL_LEAGUE_ID_LENGTH)
	private String externalLeagueId;

	@Column(name = "league_name", length = MAX_LEAGUE_NAME_LENGTH)
	private String leagueName;

	@Column(length = MAX_SEASON_LENGTH)
	private String season;

	@Column(name = "round", length = MAX_ROUND_LENGTH)
	private String round;

	@Column(name = "home_team_name", nullable = false, length = MAX_TEAM_NAME_LENGTH)
	private String homeTeamName;

	@Column(name = "away_team_name", nullable = false, length = MAX_TEAM_NAME_LENGTH)
	private String awayTeamName;

	@Column(length = MAX_VENUE_LENGTH)
	private String venue;

	@Column(length = MAX_COUNTRY_LENGTH)
	private String country;

	@Column(name = "scheduled_at")
	private Instant scheduledAt;

	@Column(name = "home_score")
	private Integer homeScore;

	@Column(name = "away_score")
	private Integer awayScore;

	@Column(name = "raw_status", length = MAX_RAW_STATUS_LENGTH)
	private String rawStatus;

	@Enumerated(EnumType.STRING)
	@Column(
		name = "normalized_status",
		nullable = false,
		columnDefinition = "enum('SCHEDULED','LIVE','FINISHED','POSTPONED','CANCELLED','SUSPENDED','UNKNOWN')"
	)
	private NormalizedStatus normalizedStatus = NormalizedStatus.UNKNOWN;

	@Column(name = "last_checked_at")
	private Instant lastCheckedAt;

	private SportEvent(
		Content content,
		SportType sportType,
		String externalLeagueId,
		String leagueName,
		String season,
		String round,
		String homeTeamName,
		String awayTeamName,
		String venue,
		String country,
		Instant scheduledAt,
		Integer homeScore,
		Integer awayScore,
		String rawStatus,
		NormalizedStatus normalizedStatus,
		Instant lastCheckedAt
	) {
		this.content = validateSportContent(content);
		this.sportType = Objects.requireNonNull(sportType, "sportType은 필수입니다.");
		this.externalLeagueId = normalizeOptional(
			externalLeagueId,
			MAX_EXTERNAL_LEAGUE_ID_LENGTH,
			"externalLeagueId"
		);
		this.leagueName = normalizeOptional(leagueName, MAX_LEAGUE_NAME_LENGTH, "leagueName");
		this.season = normalizeOptional(season, MAX_SEASON_LENGTH, "season");
		this.round = normalizeOptional(round, MAX_ROUND_LENGTH, "round");
		this.homeTeamName = normalizeRequired(homeTeamName, MAX_TEAM_NAME_LENGTH, "homeTeamName");
		this.awayTeamName = normalizeRequired(awayTeamName, MAX_TEAM_NAME_LENGTH, "awayTeamName");
		this.venue = normalizeOptional(venue, MAX_VENUE_LENGTH, "venue");
		this.country = normalizeOptional(country, MAX_COUNTRY_LENGTH, "country");
		this.scheduledAt = scheduledAt;
		validateScores(homeScore, awayScore);
		this.homeScore = homeScore;
		this.awayScore = awayScore;
		this.rawStatus = normalizeOptional(rawStatus, MAX_RAW_STATUS_LENGTH, "rawStatus");
		this.normalizedStatus = normalizedStatus == null
			? NormalizedStatus.UNKNOWN
			: normalizedStatus;
		this.lastCheckedAt = lastCheckedAt;
	}

	public static SportEvent create(
		Content content,
		SportType sportType,
		String externalLeagueId,
		String leagueName,
		String season,
		String round,
		String homeTeamName,
		String awayTeamName,
		String venue,
		String country,
		Instant scheduledAt,
		Integer homeScore,
		Integer awayScore,
		String rawStatus,
		NormalizedStatus normalizedStatus,
		Instant lastCheckedAt
	) {
		return new SportEvent(
			content,
			sportType,
			externalLeagueId,
			leagueName,
			season,
			round,
			homeTeamName,
			awayTeamName,
			venue,
			country,
			scheduledAt,
			homeScore,
			awayScore,
			rawStatus,
			normalizedStatus,
			lastCheckedAt
		);
	}

	public void updateScore(Integer homeScore, Integer awayScore) {
		validateScores(homeScore, awayScore);
		this.homeScore = homeScore;
		this.awayScore = awayScore;
	}

	public void updateDetails(
		SportType sportType,
		String leagueName,
		String season,
		String round,
		String homeTeamName,
		String awayTeamName,
		String venue,
		String country,
		Instant scheduledAt,
		Integer homeScore,
		Integer awayScore
	) {
		this.sportType = Objects.requireNonNull(sportType, "sportType은 필수입니다.");
		this.leagueName = normalizeOptional(leagueName, MAX_LEAGUE_NAME_LENGTH, "leagueName");
		this.season = normalizeOptional(season, MAX_SEASON_LENGTH, "season");
		this.round = normalizeOptional(round, MAX_ROUND_LENGTH, "round");
		this.homeTeamName = normalizeRequired(homeTeamName, MAX_TEAM_NAME_LENGTH, "homeTeamName");
		this.awayTeamName = normalizeRequired(awayTeamName, MAX_TEAM_NAME_LENGTH, "awayTeamName");
		this.venue = normalizeOptional(venue, MAX_VENUE_LENGTH, "venue");
		this.country = normalizeOptional(country, MAX_COUNTRY_LENGTH, "country");
		this.scheduledAt = scheduledAt;
		updateScore(homeScore, awayScore);
	}

	public void updateStatus(
		String rawStatus,
		NormalizedStatus normalizedStatus,
		Instant lastCheckedAt
	) {
		this.rawStatus = normalizeOptional(rawStatus, MAX_RAW_STATUS_LENGTH, "rawStatus");
		this.normalizedStatus = Objects.requireNonNull(
			normalizedStatus,
			"normalizedStatus는 필수입니다."
		);
		this.lastCheckedAt = lastCheckedAt;
	}

	private static Content validateSportContent(Content content) {
		if (content == null || content.getType() != ContentType.SPORT) {
			throw new IllegalArgumentException("스포츠 이벤트는 SPORT 콘텐츠에 속해야 합니다.");
		}
		return content;
	}

	private static void validateScores(Integer homeScore, Integer awayScore) {
		if ((homeScore == null) != (awayScore == null)
			|| (homeScore != null && (homeScore < 0 || awayScore < 0))) {
			throw new IllegalArgumentException("점수는 함께 지정해야 하며 음수일 수 없습니다.");
		}
	}

	private static String normalizeRequired(String value, int maxLength, String fieldName) {
		String normalized = normalizeOptional(value, maxLength, fieldName);
		if (normalized == null) {
			throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
		}
		return normalized;
	}

	private static String normalizeOptional(String value, int maxLength, String fieldName) {
		if (value == null) {
			return null;
		}

		String normalized = value.strip();
		if (normalized.isEmpty()) {
			return null;
		}
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(
				fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다."
			);
		}
		return normalized;
	}

	public enum NormalizedStatus {
		SCHEDULED,
		LIVE,
		FINISHED,
		POSTPONED,
		CANCELLED,
		SUSPENDED,
		UNKNOWN
	}
}
