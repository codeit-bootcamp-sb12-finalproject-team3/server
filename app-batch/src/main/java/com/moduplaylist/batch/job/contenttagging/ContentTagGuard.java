package com.moduplaylist.batch.job.contenttagging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentTagInput;
import com.moduplaylist.core.content.ai.ContentTaggingException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ContentTagGuard {
    private final ObjectMapper parser;
    private final Validator validator;
    private final ContentTaggingProperties properties;
    private static final java.util.regex.Pattern UNSAFE = java.util.regex.Pattern.compile(
        "(?i)(https?://|www\\.|[<>]|[\\p{Cc}\\p{Cf}])");
    private static final java.util.regex.Pattern INSTRUCTION = java.util.regex.Pattern.compile(
        "(?i)(ignore.{0,30}instructions?|system\\s*prompt|api[ _-]?key|비밀.{0,10}출력|"
            + "지시.{0,10}무시|명령.{0,10}실행|프롬프트|클릭|구독|무료 다운로드)");

    public ContentTagGuard(ObjectMapper mapper, Validator validator, ContentTaggingProperties properties) {
        this.parser = mapper.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.validator = validator;
        this.properties = properties;
    }

    public static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC)
            .strip().replaceAll("\\s+", " ");
    }

    public static String inputText(String value, int maxLength) {
        String text = normalize(value).replaceAll("<[^>]*>", "")
            .replaceAll("[\\p{Cc}\\p{Cf}]", "");
        return text.substring(0, Math.min(text.length(), maxLength));
    }

    public static boolean usableKeyword(String value) {
        if (value == null || UNSAFE.matcher(value).find() || INSTRUCTION.matcher(value).find()) return false;
        String normalized = normalize(value);
        return !normalized.isEmpty() && normalized.length() <= 80;
    }

    public String canonical(String value) {
        String normalized = normalize(value);
        return normalize(properties.getSynonyms().getOrDefault(normalized, normalized));
    }

    public List<String> validate(String response, ContentTagInput input) {
        if (response == null || response.getBytes(StandardCharsets.UTF_8).length > 4096) {
            throw invalidJson();
        }
        JsonNode root;
        try (JsonParser stream = parser.createParser(response)) {
            root = parser.readTree(stream);
            if (stream.nextToken() != null) throw invalidJson();
        } catch (java.io.IOException exception) {
            throw invalidJson();
        }
        if (root == null || !root.isObject() || root.size() != 1 || !root.has("tags")
            || !root.get("tags").isArray() || root.get("tags").size() > 3) throw invalidJson();

        // Complete structural validation before evaluating any candidate's meaning or length.
        List<Candidate> candidates = new ArrayList<>();
        for (JsonNode tag : root.get("tags")) {
            if (!tag.isObject() || tag.size() != 3 || !textual(tag, "name")
                || !textual(tag, "evidenceField") || !textual(tag, "evidenceText")) throw invalidJson();
            candidates.add(new Candidate(tag.get("name").textValue(), tag.get("evidenceField").textValue(),
                tag.get("evidenceText").textValue()));
        }
        if (candidates.isEmpty()) return List.of();

        Set<String> excluded = new HashSet<>();
        input.genres().forEach(value -> excluded.add(key(value)));
        input.existingTags().forEach(value -> excluded.add(key(value)));
        excluded.add(key(input.title()));
        List.of("영화", "TV 시즌", "TV 시리즈", "드라마", "애니메이션", "다큐멘터리",
            "액션", "코미디", "공포", "스릴러", "로맨스", "판타지", "movie", "tvSeason",
            "명작", "최고", "인기", "추천").forEach(value -> excluded.add(key(value)));
        Set<String> names = new LinkedHashSet<>();
        Set<String> uniqueKeys = new HashSet<>();
        for (Candidate raw : candidates) {
            if (UNSAFE.matcher(raw.name()).find() || INSTRUCTION.matcher(raw.name()).find()) continue;
            Candidate candidate = new Candidate(normalize(raw.name()), raw.evidenceField(), raw.evidenceText());
            if (!validator.validate(candidate).isEmpty() || !supported(candidate, input)) continue;
            String name = canonical(candidate.name());
            Candidate canonicalCandidate = new Candidate(name, candidate.evidenceField(), candidate.evidenceText());
            if (!validator.validate(canonicalCandidate).isEmpty() || INSTRUCTION.matcher(name).find()
                || excluded.contains(key(name)) || !uniqueKeys.add(key(name))) continue;
            names.add(name);
        }
        if (names.isEmpty()) throw new ContentTaggingException("VALIDATION_FAILED", false, false);
        return List.copyOf(names);
    }

    private boolean supported(Candidate candidate, ContentTagInput input) {
        String evidence = candidate.evidenceText();
        if (INSTRUCTION.matcher(evidence).find()) return false;
        if ("tmdbKeywords".equals(candidate.evidenceField())) {
            return "tmdbKeywords".equals(candidate.evidenceField()) && input.tmdbKeywords().contains(evidence);
        }
        return "description".equals(candidate.evidenceField())
            && input.description().contains(normalize(evidence));
    }

    private String key(String value) { return canonical(value).toLowerCase(Locale.ROOT); }
    private static boolean textual(JsonNode node, String field) {
        return node.has(field) && node.get(field).isTextual();
    }
    private static ContentTaggingException invalidJson() {
        return new ContentTaggingException("INVALID_JSON", true, false);
    }

    private record Candidate(
        @NotBlank @Size(min = 2, max = 20)
        @Pattern(regexp = "(?=.*[가-힣])[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*") String name,
        @Pattern(regexp = "description|tmdbKeywords") String evidenceField,
        @NotBlank @Size(max = 160) String evidenceText
    ) { }
}
