package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.repository.ContentRelationRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentIndexSourceLoader {

    private final ContentRepository contentRepository;
    private final ContentRelationRepository contentRelationRepository;
    private final SportEventRepository sportEventRepository;

    @Transactional(readOnly = true)
    public ContentIndexSource load(UUID contentId) {
        Content content = contentRepository.findById(contentId).orElse(null);
        if (content == null
                || content.getType() == ContentType.TV_SERIES
                || !content.isPubliclyVisible()) {
            return ContentIndexSource.notIndexable(contentId);
        }

        Content parent = content.getType() == ContentType.TV_SEASON
                ? content.getParentContent()
                : null;
        if (parent != null && parent.isHidden()) {
            return ContentIndexSource.notIndexable(contentId);
        }
        if (content.getType() == ContentType.SPORT) {
            SportEvent event = sportEventRepository
                    .findWithSportTypeByContentId(contentId)
                    .orElse(null);
            return source(content, parent, List.of(), List.of(), List.of(), sportFields(event));
        }

        List<String> castNames = contentRelationRepository.casts(contentId).stream()
                .map(ContentRelationRepository.Cast::getName)
                .toList();
        List<String> genres = contentRelationRepository.genres(contentId).stream()
                .map(ContentRelationRepository.Genre::getName)
                .toList();
        List<String> tags = contentRelationRepository.tags(contentId).stream()
                .map(ContentRelationRepository.Tag::getName)
                .toList();
        return source(content, parent, castNames, genres, tags, null);
    }

    private ContentIndexSource source(
            Content content,
            Content parent,
            List<String> castNames,
            List<String> genres,
            List<String> tags,
            ContentIndexSource.SportFields sport
    ) {
        return new ContentIndexSource(
                content.getId(),
                true,
                content.getType(),
                content.getTitle(),
                parent == null ? content.getOriginalTitle() : parent.getOriginalTitle(),
                englishTitle(content, parent),
                parent == null ? null : parent.getTitle(),
                content.getSeasonNumber(),
                content.getDescription(),
                castNames,
                genres,
                tags,
                sport
        );
    }

    private String englishTitle(Content content, Content parent) {
        if (parent == null) {
            return content.getEnglishTitle();
        }
        String parentEnglishTitle = parent.getEnglishTitle();
        return parentEnglishTitle == null ? content.getEnglishTitle() : parentEnglishTitle;
    }

    private ContentIndexSource.SportFields sportFields(SportEvent event) {
        if (event == null) {
            return null;
        }
        return new ContentIndexSource.SportFields(
                event.getSportType().getCode(),
                event.getSportType().getName(),
                event.getLeagueName(),
                event.getSeason(),
                event.getHomeTeamName(),
                event.getAwayTeamName()
        );
    }
}
