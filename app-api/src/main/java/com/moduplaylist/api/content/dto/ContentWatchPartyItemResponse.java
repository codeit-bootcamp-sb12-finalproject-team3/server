package com.moduplaylist.api.content.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentWatchPartyItemResponse {

	private UUID id;
	private String title;
	private WatchPartyDisplayStatus displayStatus;
	private Instant scheduledAt;
	private int participantCount;
	private int maxParticipants;
}
