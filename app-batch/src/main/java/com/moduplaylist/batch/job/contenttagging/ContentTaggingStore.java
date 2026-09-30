package com.moduplaylist.batch.job.contenttagging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentTagInput;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.Content.AiTaggingStatus;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.TagSource;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.content.repository.TagRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentTaggingStore {
    private static final int SIBLING_CANDIDATE_QUERY_LIMIT = 10;
    private final ContentRepository contents;
    private final ContentGenreRepository genres;
    private final ContentTagRepository contentTags;
    private final TagRepository tags;
    private final ContentTagGuard guard;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Snapshot load(UUID id) {
        Content content = lockTarget(id);
        return content == null ? null : snapshot(content, true);
    }

    /** LLM calls never run inside this transaction. False means the snapshot is no longer current. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(UUID id, Snapshot expected, List<String> names) {
        Content content = lockTarget(id);
        if (content == null || !snapshot(content, false).fingerprint().equals(expected.fingerprint())) return false;
        if (names.size() > 3 || names.stream().distinct().count() != names.size()) {
            throw new IllegalArgumentException("Invalid verified tags");
        }
        if (!names.isEmpty()) {
            names.stream().sorted().forEach(name -> tags.upsert(UUID.randomUUID(), name));
            var resolved = tags.findAllByNameIn(names);
            if (resolved.size() != names.size()) throw new IllegalStateException("Tag resolution mismatch");
            contentTags.saveAll(resolved.stream().map(tag -> ContentTag.create(content, tag, TagSource.AI)).toList());
            content.markEmbeddingSourceUpdated();
        }
        content.updateAiTaggingStatus(names.isEmpty() ? AiTaggingStatus.FAILED
            : names.size() == 3 ? AiTaggingStatus.COMPLETED : AiTaggingStatus.COMPLETED_PARTIAL);
        return true;
    }

    private Content lockTarget(UUID id) {
        UUID parentId = contents.findParentId(id).orElse(null);
        Content parent = parentId == null ? null : contents.findByIdForUpdate(parentId).orElse(null);
        Content content = contents.findByIdForUpdate(id).orElse(null);
        if (content == null || content.isHidden() || !"TMDB".equals(content.getExternalSource())
            || content.getAiTaggingStatus() != AiTaggingStatus.PENDING
            || (content.getType() != ContentType.MOVIE && content.getType() != ContentType.TV_SEASON)) return null;
        if (content.getType() == ContentType.TV_SEASON && (parent == null || parent.isHidden()
            || !Objects.equals(content.getParentContent().getId(), parentId))) return null;
        return content;
    }

    private Snapshot snapshot(Content content, boolean includeCandidates) {
        Content owner = content.getType() == ContentType.TV_SEASON ? content.getParentContent() : content;
        Map<String, Object> metadata = owner.getMetadata() == null ? Map.of() : owner.getMetadata();
        List<String> keywords = metadata.get("tmdbKeywords") instanceof List<?> entries
            ? entries.stream().filter(Map.class::isInstance).map(Map.class::cast)
                .map(item -> item.get("name")).filter(String.class::isInstance).map(String.class::cast)
                .filter(ContentTagGuard::usableKeyword).map(ContentTagGuard::normalize).distinct().limit(30).toList()
            : List.of();
        var genreRelations = genres.findAllWithGenreByContentIdIn(List.of(content.getId()));
        List<String> genreNames = genreRelations.stream()
            .map(relation -> relation.getGenre().getName()).sorted()
            .map(name -> ContentTagGuard.inputText(name, 80)).distinct().limit(20).toList();
        List<String> current = contentTags.findAllWithTagByContentIdIn(List.of(content.getId())).stream()
            .map(relation -> relation.getTag().getName()).sorted().toList();
        List<String> seriesCandidates = includeCandidates
            ? seriesTagCandidates(content, current) : List.of();
        ContentTagInput input = new ContentTagInput(content.getType().getValue(),
            ContentTagGuard.inputText(content.getTitle(), 255), genreNames,
            ContentTagGuard.inputText(content.getDescription(), 4000), keywords,
            content.getType() == ContentType.TV_SEASON ? "SERIES" : "MOVIE", current,
            seriesCandidates);
        boolean fetched = TmdbKeywordService.successful(metadata);
        try {
            // Candidate rankings are mutable global context, not source state for this content.
            var fingerprintSource = new FingerprintSource(input.type(), input.title(), input.genres(),
                input.description(), input.tmdbKeywords(), input.keywordScope(), input.currentTags(),
                owner.getId(), fetched);
            byte[] bytes = mapper.writeValueAsBytes(fingerprintSource);
            String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return new Snapshot(input, fingerprint);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot build tagging snapshot", exception);
        }
    }

    private List<String> seriesTagCandidates(Content content, List<String> currentTags) {
        if (content.getType() != ContentType.TV_SEASON || content.getParentContent() == null) {
            return List.of();
        }
        Map<String, String> candidates = new LinkedHashMap<>();
        Set<String> excludedKeys = currentTags.stream().map(this::canonicalKey)
            .collect(Collectors.toSet());
        addSeriesCandidates(candidates, excludedKeys,
            contentTags.findCanonicalNamesFromSiblingSeasons(content.getParentContent().getId(),
                content.getId(), TagSource.MANUAL,
                PageRequest.of(0, SIBLING_CANDIDATE_QUERY_LIMIT)));
        return List.copyOf(candidates.values());
    }

    private void addSeriesCandidates(Map<String, String> candidates, Set<String> excludedKeys,
                                     List<String> names) {
        for (String name : names) {
            String canonical = guard.canonical(name);
            String key = canonicalKey(canonical);
            if (!canonical.isBlank() && !excludedKeys.contains(key)) candidates.putIfAbsent(key, canonical);
            if (candidates.size() == 1) return;
        }
    }

    private String canonicalKey(String value) {
        return guard.canonical(value).toLowerCase(Locale.ROOT);
    }

    public record Snapshot(ContentTagInput input, String fingerprint) { }

    private record FingerprintSource(
        String type,
        String title,
        List<String> genres,
        String description,
        List<String> tmdbKeywords,
        String keywordScope,
        List<String> currentTags,
        UUID ownerId,
        boolean keywordsFetched
    ) { }
}
