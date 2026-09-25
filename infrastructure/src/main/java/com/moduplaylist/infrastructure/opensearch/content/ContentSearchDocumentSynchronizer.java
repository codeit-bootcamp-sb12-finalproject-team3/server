package com.moduplaylist.infrastructure.opensearch.content;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentSearchDocumentSynchronizer {

    private final ObjectProvider<ContentSearchDocumentRepository> repositoryProvider;

    public void synchronize(ContentIndexSource source) {
        ContentSearchDocumentRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            return;
        }
        if (!source.indexable()) {
            repository.deleteById(source.contentId());
            return;
        }

        ContentIndexSource.SportFields sport = source.sport();
        repository.upsertSearchFields(ContentSearchFields.builder()
                .contentId(source.contentId())
                .type(source.type().getValue())
                .title(source.title())
                .seriesTitle(source.seriesTitle())
                .originalTitle(source.originalTitle())
                .castNames(source.castNames())
                .description(source.description())
                .hidden(false)
                .genres(source.genres())
                .tags(source.tags())
                .tagSearch(source.tags())
                .sportTypeCode(sport == null ? null : sport.sportTypeCode())
                .sportType(sport == null ? null : sport.sportType())
                .leagueName(sport == null ? null : sport.leagueName())
                .season(sport == null ? null : sport.season())
                .homeTeamName(sport == null ? null : sport.homeTeamName())
                .awayTeamName(sport == null ? null : sport.awayTeamName())
                .build());
    }
}
