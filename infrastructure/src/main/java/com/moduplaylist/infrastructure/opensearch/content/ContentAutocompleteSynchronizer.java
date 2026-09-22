package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRelationRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
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
public class ContentAutocompleteSynchronizer {

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
			.build());
	}

	private List<ContentAutocompleteTerm> buildSuggestions(Content content) {
		Map<String, ContentAutocompleteTerm> suggestions = new LinkedHashMap<>();
		addSuggestion(suggestions, content.getTitle(), "title");
		if (content.getType() == ContentType.TV_SEASON) {
			addTvSeasonTitleSuggestions(suggestions, content);
		} else {
			addSuggestion(suggestions, content.getOriginalTitle(), "title");
		}
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

	private void addTvSeasonTitleSuggestions(
		Map<String, ContentAutocompleteTerm> suggestions,
		Content season
	) {
		Content series = season.getParentContent();
		addSuggestion(suggestions, series.getTitle(), "title");
		addSuggestion(suggestions, series.getTitle() + " " + season.getTitle(), "title");

		String originalTitle = series.getOriginalTitle();
		addSuggestion(suggestions, originalTitle, "title");
		if (originalTitle != null) {
			addSuggestion(
				suggestions,
				originalTitle + " Season " + season.getSeasonNumber(),
				"title"
			);
		}
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
