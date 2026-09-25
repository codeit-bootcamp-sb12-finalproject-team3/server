package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.entity.ContentType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentAutocompleteSynchronizer {

    private final ObjectProvider<ContentAutocompleteIndexRepository> repositoryProvider;

    public void synchronize(ContentIndexSource source) {
        ContentAutocompleteIndexRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            return;
        }
        if (!source.indexable()) {
            repository.deleteById(source.contentId());
            return;
        }
        repository.upsert(ContentAutocompleteDocument.builder()
                .contentId(source.contentId())
                .type(source.type().getValue())
                .suggestions(buildSuggestions(source))
                .build());
    }

    private List<ContentAutocompleteTerm> buildSuggestions(ContentIndexSource source) {
        Map<String, ContentAutocompleteTerm> suggestions = new LinkedHashMap<>();
        addSuggestion(suggestions, source.title(), "title");
        if (source.type() == ContentType.TV_SEASON) {
            addTvSeasonTitleSuggestions(suggestions, source);
        } else {
            addSuggestion(suggestions, source.originalTitle(), "title");
        }
        source.castNames().forEach(name -> addSuggestion(suggestions, name, "cast"));
        source.genres().forEach(name -> addSuggestion(suggestions, name, "genre"));
        source.tags().forEach(name -> addSuggestion(suggestions, name, "tag"));

        ContentIndexSource.SportFields sport = source.sport();
        if (sport != null) {
            addSuggestion(suggestions, sport.sportTypeCode(), "sportType");
            addSuggestion(suggestions, sport.sportType(), "sportType");
            addSuggestion(suggestions, sport.leagueName(), "league");
            addSuggestion(suggestions, sport.season(), "season");
            addSuggestion(suggestions, sport.homeTeamName(), "team");
            addSuggestion(suggestions, sport.awayTeamName(), "team");
        }
        return List.copyOf(suggestions.values());
    }

    private void addTvSeasonTitleSuggestions(
            Map<String, ContentAutocompleteTerm> suggestions,
            ContentIndexSource source
    ) {
        addSuggestion(suggestions, source.seriesTitle(), "title");
        if (source.seriesTitle() != null
                && !source.title().startsWith(source.seriesTitle())) {
            addSuggestion(
                    suggestions,
                    source.seriesTitle() + " " + source.title(),
                    "title"
            );
        }
        addSuggestion(suggestions, source.originalTitle(), "title");
        if (source.originalTitle() != null) {
            addSuggestion(
                    suggestions,
                    source.originalTitle() + " Season " + source.seasonNumber(),
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
