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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentTaggingStore {
    private final ContentRepository contents;
    private final ContentGenreRepository genres;
    private final ContentTagRepository contentTags;
    private final TagRepository tags;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Snapshot load(UUID id) {
        Content content = lockTarget(id);
        return content == null ? null : snapshot(content);
    }

    /** LLM calls never run inside this transaction. False means the snapshot is no longer current. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(UUID id, Snapshot expected, List<String> names) {
        Content content = lockTarget(id);
        if (content == null || !snapshot(content).fingerprint().equals(expected.fingerprint())) return false;
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

    private Snapshot snapshot(Content content) {
        Content owner = content.getType() == ContentType.TV_SEASON ? content.getParentContent() : content;
        Map<String, Object> metadata = owner.getMetadata() == null ? Map.of() : owner.getMetadata();
        List<String> keywords = metadata.get("tmdbKeywords") instanceof List<?> entries
            ? entries.stream().filter(Map.class::isInstance).map(Map.class::cast)
                .map(item -> item.get("name")).filter(String.class::isInstance).map(String.class::cast)
                .filter(ContentTagGuard::usableKeyword).map(ContentTagGuard::normalize).distinct().limit(30).toList()
            : List.of();
        List<String> genreNames = genres.findAllWithGenreByContentIdIn(List.of(content.getId())).stream()
            .map(relation -> relation.getGenre().getName()).sorted()
            .map(name -> ContentTagGuard.inputText(name, 80)).distinct().limit(20).toList();
        List<String> existing = contentTags.findAllWithTagByContentIdIn(List.of(content.getId())).stream()
            .map(relation -> relation.getTag().getName()).sorted().toList();
        ContentTagInput input = new ContentTagInput(content.getType().getValue(),
            ContentTagGuard.inputText(content.getTitle(), 255), genreNames,
            ContentTagGuard.inputText(content.getDescription(), 4000), keywords,
            content.getType() == ContentType.TV_SEASON ? "SERIES" : "MOVIE", existing);
        boolean fetched = TmdbKeywordService.successful(metadata);
        try {
            // Include identity and fetch status so a moved season or completed backfill invalidates old results.
            byte[] bytes = mapper.writeValueAsBytes(List.of(input, owner.getId(), fetched));
            String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return new Snapshot(input, fingerprint);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot build tagging snapshot", exception);
        }
    }

    public record Snapshot(ContentTagInput input, String fingerprint) { }
}
