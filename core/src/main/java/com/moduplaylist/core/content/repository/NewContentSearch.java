package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public class NewContentSearch {

	private final Instant createdAtFrom;
	private final Instant cursorCreatedAt;
	private final UUID idAfter;
	private final int limit;

	public NewContentSearch(
		Instant createdAtFrom,
		Instant cursorCreatedAt,
		UUID idAfter,
		int limit
	) {
		if (createdAtFrom == null
			|| (cursorCreatedAt == null) != (idAfter == null)
			|| limit < 1
			|| limit > 100) {
			throw new InvalidContentSearchException();
		}
		this.createdAtFrom = createdAtFrom;
		this.cursorCreatedAt = cursorCreatedAt;
		this.idAfter = idAfter;
		this.limit = limit;
	}
}
