package com.moduplaylist.infrastructure.opensearch.content;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentAutocompleteDocument {

	private UUID contentId;
	private String type;
	private List<ContentAutocompleteTerm> suggestions;
}
