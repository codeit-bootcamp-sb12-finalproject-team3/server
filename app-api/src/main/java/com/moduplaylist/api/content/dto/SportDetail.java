package com.moduplaylist.api.content.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SportDetail {

	private SportTypeResponse sportType;
	private Instant scheduledAt;
	private String league;
	private String season;
	private String round;
	private String homeTeam;
	private String awayTeam;
	private String venue;
	private String country;
	private Integer homeScore;
	private Integer awayScore;
}
