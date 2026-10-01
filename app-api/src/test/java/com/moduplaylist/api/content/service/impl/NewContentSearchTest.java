package com.moduplaylist.api.content.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import com.moduplaylist.core.content.repository.NewContentSearch;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NewContentSearchTest {

	private static final Instant CREATED_AT_FROM = Instant.parse("2026-08-24T00:00:00Z");
	private static final Instant CURSOR = Instant.parse("2026-09-22T00:00:00Z");
	private static final UUID ID_AFTER = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Test
	void cursorAndIdAfterMustBeProvidedTogether() {
		assertThatThrownBy(() -> new NewContentSearch(CREATED_AT_FROM, CURSOR, null, 20))
			.isInstanceOf(InvalidContentSearchException.class);
		assertThatThrownBy(() -> new NewContentSearch(CREATED_AT_FROM, null, ID_AFTER, 20))
			.isInstanceOf(InvalidContentSearchException.class);
	}

	@Test
	void limitMustBeBetweenOneAndOneHundred() {
		assertThatThrownBy(() -> new NewContentSearch(CREATED_AT_FROM, null, null, 0))
			.isInstanceOf(InvalidContentSearchException.class);
		assertThatThrownBy(() -> new NewContentSearch(CREATED_AT_FROM, null, null, 101))
			.isInstanceOf(InvalidContentSearchException.class);
	}
}
