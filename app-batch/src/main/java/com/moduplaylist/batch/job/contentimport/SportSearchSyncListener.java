package com.moduplaylist.batch.job.contentimport;

import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteIndexRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteTerm;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportSearchSyncListener {
    private final ContentRepository contentRepository;
    private final SportEventRepository sportEventRepository;
    private final ContentEmbeddingService contentEmbeddingService;
    private final ObjectProvider<ContentAutocompleteIndexRepository> autocompleteRepositoryProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void synchronize(SportSearchSyncRequested event) {
        try {
            contentEmbeddingService.indexSportSearchDocument(event.contentId());
        } catch (RuntimeException exception) {
            log.warn("스포츠 콘텐츠 검색 문서 동기화에 실패했습니다. contentId={}",
                event.contentId(), exception);
        }
        try {
            synchronizeAutocomplete(event);
        } catch (RuntimeException exception) {
            log.warn("스포츠 콘텐츠 자동완성 동기화에 실패했습니다. contentId={}",
                event.contentId(), exception);
        }
    }

    private void synchronizeAutocomplete(SportSearchSyncRequested event) {
        ContentAutocompleteIndexRepository repository = autocompleteRepositoryProvider.getIfAvailable();
        if (repository == null) return;
        Content content = contentRepository.findById(event.contentId()).orElse(null);
        SportEvent sportEvent = sportEventRepository.findWithSportTypeByContentId(event.contentId()).orElse(null);
        if (content == null || sportEvent == null || content.isHidden()) return;

        Map<String, ContentAutocompleteTerm> suggestions = new LinkedHashMap<>();
        add(suggestions, content.getTitle(), "title");
        add(suggestions, sportEvent.getSportType().getCode(), "sportType");
        add(suggestions, sportEvent.getSportType().getName(), "sportType");
        add(suggestions, sportEvent.getLeagueName(), "league");
        add(suggestions, sportEvent.getSeason(), "season");
        add(suggestions, sportEvent.getHomeTeamName(), "team");
        add(suggestions, sportEvent.getAwayTeamName(), "team");
        repository.upsert(ContentAutocompleteDocument.builder()
            .contentId(content.getId())
            .type(content.getType().getValue())
            .suggestions(List.copyOf(suggestions.values()))
            .build());
    }

    private static void add(
        Map<String, ContentAutocompleteTerm> suggestions,
        String value,
        String type
    ) {
        if (value == null || value.isBlank()) return;
        String normalized = value.strip();
        suggestions.putIfAbsent(
            normalized.toLowerCase(Locale.ROOT),
            new ContentAutocompleteTerm(normalized, type)
        );
    }
}
