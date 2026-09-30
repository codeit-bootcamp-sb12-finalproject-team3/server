package com.moduplaylist.infrastructure.opensearch.content;

import java.util.UUID;

public record ContentAutocompleteCandidate(
	UUID contentId,
	String text,
	String type,
	int matchRank
) {
}
