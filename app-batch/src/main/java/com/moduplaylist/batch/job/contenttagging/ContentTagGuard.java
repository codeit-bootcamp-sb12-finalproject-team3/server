package com.moduplaylist.batch.job.contenttagging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentExternalEvidence;
import com.moduplaylist.core.content.ai.ContentTagInput;
import com.moduplaylist.core.content.ai.ContentTaggingException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
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
    private static final java.util.regex.Pattern EVIDENCE_INJECTION = java.util.regex.Pattern.compile(
        "(?i)(ignore.{0,30}instructions?|system\\s*prompt|api[ _-]?key|"
            + "지시.{0,10}무시|명령.{0,10}실행|프롬프트)");
    private static final java.util.regex.Pattern UNSAFE_SOURCE_TITLE = java.util.regex.Pattern.compile(
        "[<>\\p{Cc}\\p{Cf}]");
    private static final java.util.regex.Pattern ENGLISH_TOKEN = java.util.regex.Pattern.compile(
        "[A-Za-z]+(?:-[A-Za-z]+)*");
    private static final java.util.regex.Pattern INSTALLMENT_LABEL = java.util.regex.Pattern.compile(
        "^(?:제 ?)?(?:[0-9]+|[일이삼사오육칠팔구십]+) ?(?:부작|부|장|화|편|시즌)$|^시즌 ?[0-9]+$");
    private static final java.util.regex.Pattern ADULT_CHILD_CONTEXT = java.util.regex.Pattern.compile(
        "다 큰 (?:아들|딸|자녀)|성인 (?:아들|딸|자녀)|[0-9]{3,4}개월");
    private static final java.util.regex.Pattern EMPTY_GENRE_PHRASE = java.util.regex.Pattern.compile(
        "^(?:판타지|액션|SF|드라마|애니메이션|코미디|공포|스릴러|로맨스|모험|범죄|전쟁|가족)"
            + " (?:요소|설정|성격)$");
    private static final java.util.regex.Pattern TIME_TRAVEL_PREMISE = java.util.regex.Pattern.compile(
        "(?i)(시간 ?여행|시간을 (?:오가|거슬러|넘나들|되돌리)|"
            + "과거(?:나 미래)?로 (?:돌아가|이동)|타임 ?머신|"
            + "time[ -]?travel|time machine|travel(?:s|ling)? (?:through|back in) time)");
    private static final Set<String> ALLOWED_ENGLISH_TOKENS = Set.of(
        "AI", "VR", "SNS", "CIA", "FBI", "K-pop", "OTT");
    private static final Set<String> BLOCKED_EVIDENCE_DOMAINS = Set.of(
        "reddit.com", "fandom.com", "namu.wiki", "wikipedia.org");

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

    /** Checks the stored research record, not a brittle exact match against the model's paraphrase. */
    public static boolean usableExternalEvidence(ContentExternalEvidence fact, String type) {
        if (fact == null || fact.category() == null || fact.scope() == null
            || fact.text() == null || fact.sourceTitle() == null || fact.sourceUrl() == null) return false;
        String text = normalize(fact.text());
        String title = normalize(fact.sourceTitle());
        if (text.length() < 2 || text.length() > 240 || title.isEmpty() || title.length() > 160
            || UNSAFE.matcher(text).find() || EVIDENCE_INJECTION.matcher(text).find()
            || UNSAFE_SOURCE_TITLE.matcher(fact.sourceTitle()).find()
            || EVIDENCE_INJECTION.matcher(title).find()) return false;
        if ("movie".equals(type) != (fact.scope() == ContentExternalEvidence.Scope.MOVIE)) return false;
        if (!"movie".equals(type) && !"tvSeason".equals(type)) return false;
        if (fact.sourceUrl().length() > 2_000) return false;
        try {
            URI url = URI.create(fact.sourceUrl().strip());
            String host = url.getHost();
            if (host == null || url.getUserInfo() != null
                || !("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme()))) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return BLOCKED_EVIDENCE_DOMAINS.stream()
                .noneMatch(blocked -> normalizedHost.equals(blocked)
                    || normalizedHost.endsWith("." + blocked));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
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
            if (!tag.isObject() || tag.size() != 4 || !textual(tag, "name")
                || !textual(tag, "evidenceField") || !textual(tag, "evidenceText")
                || !tag.has("evidenceIndex") || !(tag.get("evidenceIndex").isNull()
                    || tag.get("evidenceIndex").isInt())) throw invalidJson();
            candidates.add(new Candidate(tag.get("name").textValue(), tag.get("evidenceField").textValue(),
                tag.get("evidenceIndex").isNull() ? null : tag.get("evidenceIndex").intValue(),
                tag.get("evidenceText").textValue()));
        }
        if (candidates.isEmpty()) return List.of();

        Set<String> excluded = new HashSet<>();
        input.genres().forEach(value -> excluded.add(key(value)));
        input.currentTags().forEach(value -> excluded.add(key(value)));
        excluded.add(key(input.title()));
        List.of("영화", "TV 시즌", "TV 시리즈", "드라마", "애니메이션", "다큐멘터리",
            "액션", "코미디", "공포", "스릴러", "로맨스", "movie", "tvSeason",
            "명작", "최고", "인기", "추천").forEach(value -> excluded.add(key(value)));
        Set<String> names = new LinkedHashSet<>();
        Set<String> uniqueKeys = new HashSet<>();
        for (Candidate raw : candidates) {
            Candidate candidate = new Candidate(normalize(raw.name()), raw.evidenceField(),
                raw.evidenceIndex(), raw.evidenceText());
            if (UNSAFE.matcher(candidate.name()).find() || INSTRUCTION.matcher(candidate.name()).find()
                || hasDisallowedEnglishToken(candidate.name())
                || !validator.validate(candidate).isEmpty() || !supported(candidate, input)
                || lowValueTag(candidate.name(), input)) continue;
            String name = canonical(candidate.name());
            Candidate canonicalCandidate = new Candidate(name, candidate.evidenceField(),
                candidate.evidenceIndex(), candidate.evidenceText());
            if (!validator.validate(canonicalCandidate).isEmpty() || INSTRUCTION.matcher(name).find()
                || hasDisallowedEnglishToken(name) || excluded.contains(key(name))
                || !specificIdentitySupported(name, candidate, input)
                || !spoilerSafe(name, input)
                || !uniqueKeys.add(key(name))) continue;
            names.add(name);
        }
        if (names.isEmpty()) throw new ContentTaggingException("VALIDATION_FAILED", false, false);
        return List.copyOf(names);
    }

    private boolean supported(Candidate candidate, ContentTagInput input) {
        String evidence = candidate.evidenceText();
        if (INSTRUCTION.matcher(evidence).find()) return false;
        if ("tmdbKeywords".equals(candidate.evidenceField())) {
            return candidate.evidenceIndex() != null && candidate.evidenceIndex() >= 0
                && candidate.evidenceIndex() < input.tmdbKeywords().size()
                && usableKeyword(input.tmdbKeywords().get(candidate.evidenceIndex()));
        }
        if ("externalEvidence".equals(candidate.evidenceField())) {
            return !UNSAFE.matcher(evidence).find() && candidate.evidenceIndex() != null
                && candidate.evidenceIndex() >= 0 && input.externalEvidence() != null
                && candidate.evidenceIndex() < input.externalEvidence().size()
                && usableExternalEvidence(input.externalEvidence().get(candidate.evidenceIndex()), input.type());
        }
        return "description".equals(candidate.evidenceField())
            && candidate.evidenceIndex() == null && !input.description().isBlank();
    }

    /** A plot device in TMDB keywords alone is not proof it is safe to reveal before viewing. */
    private boolean spoilerSafe(String name, ContentTagInput input) {
        if (!"시간 여행".equals(key(name))) return true;
        if (TIME_TRAVEL_PREMISE.matcher(input.title()).find()) return true;
        if (TIME_TRAVEL_PREMISE.matcher(input.description()).find()) return true;
        return input.externalEvidence() != null && input.externalEvidence().stream()
            .filter(fact -> usableExternalEvidence(fact, input.type()))
            .filter(fact -> fact.category() == ContentExternalEvidence.Category.SUBGENRE
                || fact.category() == ContentExternalEvidence.Category.CORE_MOTIF)
            .anyMatch(fact -> TIME_TRAVEL_PREMISE.matcher(fact.text()).find());
    }

    /** Only unambiguous identity claims receive lexical checks; other facts may be paraphrased. */
    private boolean specificIdentitySupported(String name, Candidate candidate, ContentTagInput input) {
        String source = switch (candidate.evidenceField()) {
            case "description" -> input.description();
            case "tmdbKeywords" -> input.tmdbKeywords().get(candidate.evidenceIndex());
            case "externalEvidence" -> input.externalEvidence().get(candidate.evidenceIndex()).text();
            default -> "";
        };
        String normalized = normalize(source).toLowerCase(Locale.ROOT);
        return switch (name) {
            case "소년 만화 원작" -> normalized.matches("(?s).*(소년 ?만화|shonen|shounen).*");
            case "소설 원작" -> normalized.matches("(?s).*(소설|novel|book).*");
            case "라이트 노벨 원작" -> normalized.matches("(?s).*(라이트 ?노벨|light novel).*");
            case "마블" -> normalized.matches("(?s).*(마블|marvel|\\bmcu\\b).*");
            case "리얼로봇" -> normalized.matches("(?s).*(리얼 ?로봇|real robot).*");
            case "거대로봇" -> normalized.matches("(?s).*(거대 ?로봇|giant robot|mecha|리얼 ?로봇|real robot).*");
            default -> true;
        };
    }

    private static boolean hasDisallowedEnglishToken(String value) {
        var matcher = ENGLISH_TOKEN.matcher(value);
        while (matcher.find()) {
            if (!ALLOWED_ENGLISH_TOKENS.contains(matcher.group())) return true;
        }
        return false;
    }

    private static boolean lowValueTag(String name, ContentTagInput input) {
        if (name.endsWith(" 요소") || EMPTY_GENRE_PHRASE.matcher(name).matches()
            || INSTALLMENT_LABEL.matcher(name).matches()) return true;
        if (!name.contains("육아")) return false;
        String context = input.description() + " " + input.externalEvidence().stream()
            .map(ContentExternalEvidence::text).filter(java.util.Objects::nonNull)
            .reduce("", (left, right) -> left + " " + right);
        return ADULT_CHILD_CONTEXT.matcher(context).find();
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
        @Pattern(regexp = "(?=.*[가-힣])(?:[가-힣A-Za-z0-9]+|K-pop)(?: (?:[가-힣A-Za-z0-9]+|K-pop))*") String name,
        @Pattern(regexp = "description|tmdbKeywords|externalEvidence") String evidenceField,
        Integer evidenceIndex,
        @NotBlank @Size(max = 160) String evidenceText
    ) { }
}
