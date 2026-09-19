package com.moduplaylist.api.content.service;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentRelationRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteIndexRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteTerm;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentAutocompleteIndexService {

	private static final String ORIGINAL_TITLE_METADATA_KEY = "originalTitle";

	private final ContentRepository contentRepository;
	private final SportEventRepository sportEventRepository;
	private final ContentRelationRepository contentRelationRepository;
	private final ObjectProvider<ContentAutocompleteIndexRepository> repositoryProvider;

	@Transactional(readOnly = true)
	public void synchronize(UUID contentId) {
		ContentAutocompleteIndexRepository repository = repositoryProvider.getIfAvailable();
		if (repository == null) {
			return;
		}

		Content content = contentRepository.findById(contentId).orElse(null);
		if (content == null || content.isHidden() || content.getType() == ContentType.TV_SERIES) {
			repository.deleteById(contentId);
			return;
		}

		repository.upsert(ContentAutocompleteDocument.builder()
			.contentId(content.getId())
			.type(content.getType().getValue())
			.suggestions(buildSuggestions(content))
			.hidden(false)
			.build());
	}

	private List<ContentAutocompleteTerm> buildSuggestions(Content content) {
		Map<String, ContentAutocompleteTerm> suggestions = new LinkedHashMap<>();
		addSuggestion(suggestions, content.getTitle(), "title");
		addSuggestion(suggestions, findOriginalTitle(content), "title");
		contentRelationRepository.casts(content.getId())
			.forEach(cast -> addSuggestion(suggestions, cast.getName(), "cast"));
		contentRelationRepository.genres(content.getId())
			.forEach(genre -> addSuggestion(suggestions, genre.getName(), "genre"));
		contentRelationRepository.tags(content.getId())
			.forEach(tag -> addSuggestion(suggestions, tag.getName(), "tag"));

		if (content.getType() == ContentType.SPORT) {
			sportEventRepository.findWithSportTypeByContentId(content.getId())
				.ifPresent(event -> {
					addSuggestion(suggestions, event.getSportType().getCode(), "sportType");
					addSuggestion(suggestions, event.getSportType().getName(), "sportType");
					addSuggestion(suggestions, event.getLeagueName(), "league");
					addSuggestion(suggestions, event.getSeason(), "season");
					addSuggestion(suggestions, event.getHomeTeamName(), "team");
					addSuggestion(suggestions, event.getAwayTeamName(), "team");
				});
		}

		return List.copyOf(suggestions.values());
	}

	private String findOriginalTitle(Content content) {
		Map<String, Object> metadata = content.getMetadata();
		if (metadata == null) {
			return null;
		}
		Object value = metadata.get(ORIGINAL_TITLE_METADATA_KEY);
		if (!(value instanceof String title)) {
			return null;
		}
		String normalized = title.strip();
		return normalized.isEmpty() ? null : normalized;
	}

	private void addSuggestion(
		Map<String, ContentAutocompleteTerm> suggestions,
		String text,
		String type
	) {
		if (text == null || text.isBlank()) {
			return;
		}
		String normalized = text.strip();
		suggestions.putIfAbsent(
			normalized.toLowerCase(Locale.ROOT),
			new ContentAutocompleteTerm(normalized, type)
		);
	}
}
