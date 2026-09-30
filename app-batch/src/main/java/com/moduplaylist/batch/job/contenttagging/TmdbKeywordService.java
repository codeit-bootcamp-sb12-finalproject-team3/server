package com.moduplaylist.batch.job.contenttagging;

import com.fasterxml.jackson.databind.JsonNode;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.externalapi.ExternalApiException;
import com.moduplaylist.infrastructure.tmdb.TmdbContentClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbKeywordService {
    private final TmdbContentClient client;
    private final ContentRepository contents;
    private final TransactionTemplate transactions;

    /** Called outside the content save transaction; failed requests never roll back content import. */
    public Map<String, Object> fetch(int externalId, boolean series) {
        String scope = series ? "SERIES" : "MOVIE";
        try {
            JsonNode response = series ? client.tvKeywords(externalId) : client.movieKeywords(externalId);
            JsonNode array = response.get(series ? "results" : "keywords");
            if (array == null || !array.isArray()) throw new IllegalArgumentException("INVALID_KEYWORDS");
            List<Map<String, Object>> keywords = new ArrayList<>();
            var seen = new HashSet<String>();
            for (JsonNode item : array) {
                if (!item.path("id").isIntegralNumber() || item.path("id").asInt() <= 0
                    || !item.path("name").isTextual()) continue;
                String name = item.path("name").textValue();
                if (!ContentTagGuard.usableKeyword(name)) continue;
                name = ContentTagGuard.normalize(name);
                if (seen.add(name)) keywords.add(Map.of("id", item.path("id").asInt(), "name", name));
                if (keywords.size() == 30) break;
            }
            return Map.of("tmdbKeywords", keywords, "keywordScope", scope, "keywordsFetchStatus", "SUCCESS");
        } catch (RuntimeException exception) {
            if (Thread.currentThread().isInterrupted()) throw exception;
            String reason = exception instanceof ExternalApiException external
                ? external.getFailureType().name() : "INVALID_RESPONSE";
            log.warn("TMDB 키워드 조회 실패 externalId={}, scope={}, reason={}", externalId, scope, reason);
            return Map.of("tmdbKeywords", List.of(), "keywordScope", scope, "keywordsFetchStatus", "FAILED");
        }
    }

    /** Backfills legacy PENDING records; parent metadata is shared by every season. */
    public void ensureKeywords(UUID contentId) {
        KeywordTarget target = transactions.execute(status -> {
            Content content = contents.findById(contentId).orElse(null);
            if (content == null || content.isHidden() || content.getAiTaggingStatus() != Content.AiTaggingStatus.PENDING
                || !"TMDB".equals(content.getExternalSource())) return null;
            Content owner = content.getType() == ContentType.TV_SEASON ? content.getParentContent() : content;
            if (owner == null || owner.isHidden() || !"TMDB".equals(owner.getExternalSource())
                || owner.getExternalId() == null || successful(owner.getMetadata())) return null;
            return new KeywordTarget(owner.getId(), owner.getExternalId(), owner.getType() == ContentType.TV_SERIES);
        });
        if (target == null) return;
        Map<String, Object> metadata = fetch(target.externalId(), target.series());
        transactions.executeWithoutResult(status -> contents.findByIdForUpdate(target.ownerId()).ifPresent(owner -> {
            if (!owner.isHidden() && !successful(owner.getMetadata())) owner.mergeMetadata(metadata);
        }));
    }

    public static boolean successful(Map<String, Object> metadata) {
        return metadata != null && "SUCCESS".equals(metadata.get("keywordsFetchStatus"));
    }

    private record KeywordTarget(UUID ownerId, int externalId, boolean series) { }
}
