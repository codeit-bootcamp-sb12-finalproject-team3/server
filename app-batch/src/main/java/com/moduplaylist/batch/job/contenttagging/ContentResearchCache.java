package com.moduplaylist.batch.job.contenttagging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentExternalEvidence;
import com.moduplaylist.core.content.ai.ContentResearchInput;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.infrastructure.ai.content.OpenAiContentEvidenceResearcher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One cache contract shared by research and tagging so neither trusts stale or malformed facts. */
final class ContentResearchCache {
    static final String METADATA_KEY = "tagResearch";
    static final String VERSION = OpenAiContentEvidenceResearcher.RESEARCH_VERSION;

    private ContentResearchCache() { }

    static ContentResearchInput input(Content content) {
        Content parent = content.getType() == ContentType.TV_SEASON ? content.getParentContent() : null;
        return new ContentResearchInput(content.getType().getValue(), content.getTitle(),
            firstNonBlank(content.getOriginalTitle(), parent == null ? null : parent.getOriginalTitle()),
            firstNonBlank(content.getEnglishTitle(), parent == null ? null : parent.getEnglishTitle()),
            content.getReleaseDate() == null ? null : content.getReleaseDate().getYear(),
            content.getSeasonNumber(), parent == null ? null : parent.getTitle(),
            parent == null ? content.getExternalId() : parent.getExternalId());
    }

    static String identityHash(Content content, ContentResearchInput input, ObjectMapper mapper) {
        Content parent = content.getType() == ContentType.TV_SEASON ? content.getParentContent() : null;
        Identity identity = new Identity(content.getId(), parent == null ? null : parent.getId(),
            content.getExternalId(), input);
        try {
            byte[] source = mapper.writeValueAsBytes(identity);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot build research identity", exception);
        }
    }

    static Cached read(Content content, String identityHash, Instant now) {
        return read(content, identityHash, now, 90, 6, 5);
    }

    static Cached read(Content content, String identityHash, Instant now,
                       int cacheDays, int maxFacts, int maxSources) {
        if (cacheDays <= 0 || maxFacts <= 0 || maxSources <= 0) {
            throw new IllegalArgumentException("Invalid content research cache configuration");
        }
        Map<String, Object> metadata = content.getMetadata();
        if (metadata == null || !(metadata.get(METADATA_KEY) instanceof Map<?, ?> value)
            || !VERSION.equals(value.get("version")) || !identityHash.equals(value.get("identityHash"))
            || !(value.get("facts") instanceof List<?> entries)
            || !(value.get("sources") instanceof List<?>)) return null;
        try {
            Instant fetchedAt = Instant.parse((String) value.get("fetchedAt"));
            if (fetchedAt.isAfter(now) || !fetchedAt.plus(Duration.ofDays(cacheDays)).isAfter(now)) return null;
        } catch (RuntimeException invalid) {
            return null;
        }

        String status = value.get("status") instanceof String text ? text : "";
        if ("EMPTY".equals(status)) return entries.isEmpty() ? new Cached(List.of()) : null;
        if (!"SUCCESS".equals(status) || entries.isEmpty()) return null;

        List<ContentExternalEvidence> facts = new ArrayList<>();
        Set<String> sources = new LinkedHashSet<>();
        for (Object entry : entries) {
            ContentExternalEvidence fact = parseFact(entry);
            if (ContentTagGuard.usableExternalEvidence(fact, content.getType().getValue())
                && (sources.contains(fact.sourceUrl()) || sources.size() < maxSources)) {
                sources.add(fact.sourceUrl());
                facts.add(fact);
                if (facts.size() == maxFacts) break;
            }
        }
        // A bad item must not suppress the other good facts; no valid item means re-research.
        return facts.isEmpty() ? null : new Cached(List.copyOf(facts));
    }

    static Map<String, Object> value(String identityHash, List<ContentExternalEvidence> facts, Instant now) {
        List<Map<String, String>> storedFacts = new ArrayList<>();
        Map<String, String> sources = new LinkedHashMap<>();
        for (ContentExternalEvidence fact : facts) {
            storedFacts.add(Map.of("category", fact.category().name(), "text", fact.text(),
                "scope", fact.scope().name(), "sourceTitle", fact.sourceTitle(), "sourceUrl", fact.sourceUrl()));
            sources.putIfAbsent(fact.sourceUrl(), fact.sourceTitle());
        }
        List<Map<String, String>> storedSources = sources.entrySet().stream()
            .map(entry -> Map.of("title", entry.getValue(), "url", entry.getKey())).toList();
        return Map.of("version", VERSION, "status", facts.isEmpty() ? "EMPTY" : "SUCCESS",
            "fetchedAt", now.toString(), "identityHash", identityHash,
            "facts", storedFacts, "sources", storedSources);
    }

    private static ContentExternalEvidence parseFact(Object entry) {
        if (!(entry instanceof Map<?, ?> fact)) return null;
        try {
            return new ContentExternalEvidence(
                ContentExternalEvidence.Category.valueOf((String) fact.get("category")),
                (String) fact.get("text"),
                ContentExternalEvidence.Scope.valueOf((String) fact.get("scope")),
                (String) fact.get("sourceTitle"), (String) fact.get("sourceUrl"));
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    record Cached(List<ContentExternalEvidence> facts) { }
    private record Identity(UUID contentId, UUID parentId, Integer externalId, ContentResearchInput input) { }
}
