package com.moduplaylist.infrastructure.ai.content;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentExternalEvidence;
import com.moduplaylist.core.content.ai.ContentResearchInput;
import com.moduplaylist.core.content.ai.ContentTaggingException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates model facts against URLs returned by the web search tool itself. */
public final class OpenAiContentEvidenceResponseParser {
    private static final int MAX_RESPONSE_LENGTH = 256_000;
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
        "reddit.com", "fandom.com", "namu.wiki", "wikipedia.org");
    private static final Pattern UNSAFE_TEXT = Pattern.compile(
        "(?i)(https?://|www\\.|ignore.{0,30}instructions?|system\\s*prompt|"
            + "지시.{0,10}무시|명령.{0,10}실행|프롬프트)");

    private final ObjectMapper mapper;
    private final int maxFacts;
    private final int maxDistinctSources;

    public OpenAiContentEvidenceResponseParser(ObjectMapper mapper) {
        this(mapper, 6, 5);
    }

    public OpenAiContentEvidenceResponseParser(ObjectMapper mapper, int maxFacts, int maxDistinctSources) {
        if (maxFacts <= 0 || maxDistinctSources <= 0) {
            throw new IllegalArgumentException("Invalid content research limits");
        }
        this.mapper = mapper.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.maxFacts = maxFacts;
        this.maxDistinctSources = maxDistinctSources;
    }

    public List<ContentExternalEvidence> parse(String responseBody, ContentResearchInput input) {
        if (responseBody == null || responseBody.length() > MAX_RESPONSE_LENGTH) throw invalidResponse();
        JsonNode response = readJson(responseBody);
        if (!"completed".equals(response.path("status").asText())) {
            throw new ContentTaggingException("RESEARCH_INCOMPLETE_RESPONSE", true, false);
        }
        JsonNode output = response.path("output");
        if (!output.isArray()) throw invalidResponse();

        Map<String, Source> searchedUrls = new LinkedHashMap<>();
        String factsJson = null;
        boolean searched = false;
        for (JsonNode item : output) {
            String type = item.path("type").asText();
            if ("web_search_call".equals(type)) {
                if (!"completed".equals(item.path("status").asText())) {
                    throw new ContentTaggingException("RESEARCH_INCOMPLETE_RESPONSE", true, false);
                }
                searched = true;
                for (JsonNode source : item.path("action").path("sources")) {
                    addSource(searchedUrls, source.path("url").asText(null),
                        source.path("title").asText(null));
                }
            } else if ("message".equals(type)) {
                for (JsonNode content : item.path("content")) {
                    if ("refusal".equals(content.path("type").asText())) {
                        throw new ContentTaggingException("RESEARCH_MODEL_REFUSAL", false, false);
                    }
                    if (!"output_text".equals(content.path("type").asText())) continue;
                    factsJson = content.path("text").asText(null);
                    for (JsonNode annotation : content.path("annotations")) {
                        if (!"url_citation".equals(annotation.path("type").asText())) continue;
                        String url = annotation.path("url").asText(null);
                        if (url == null) url = annotation.path("url_citation").path("url").asText(null);
                        String title = annotation.path("title").asText(null);
                        if (title == null) {
                            title = annotation.path("url_citation").path("title").asText(null);
                        }
                        addSource(searchedUrls, url, title);
                    }
                }
            }
        }
        if (!searched || factsJson == null || factsJson.length() > 16_000) throw invalidResponse();
        JsonNode factsRoot = readJson(factsJson);
        JsonNode facts = factsRoot.path("facts");
        if (!factsRoot.isObject() || !facts.isArray()) throw invalidResponse();

        List<ContentExternalEvidence> accepted = new ArrayList<>();
        Set<String> usedFacts = new LinkedHashSet<>();
        Set<String> usedSources = new LinkedHashSet<>();
        for (JsonNode fact : facts) {
            if (accepted.size() == maxFacts) break;
            ContentExternalEvidence evidence = validFact(fact, input, searchedUrls);
            if (evidence == null) continue;
            String factKey = evidence.category() + ":" + evidence.scope() + ":"
                + evidence.text().toLowerCase(Locale.ROOT);
            if (usedFacts.contains(factKey)) continue;
            if (!usedSources.contains(evidence.sourceUrl()) && usedSources.size() == maxDistinctSources) continue;
            usedFacts.add(factKey);
            usedSources.add(evidence.sourceUrl());
            accepted.add(evidence);
        }
        return List.copyOf(accepted);
    }

    private ContentExternalEvidence validFact(JsonNode fact, ContentResearchInput input,
                                               Map<String, Source> searchedUrls) {
        if (!fact.isObject()) return null;
        ContentExternalEvidence.Category category;
        ContentExternalEvidence.Scope scope;
        try {
            category = ContentExternalEvidence.Category.valueOf(fact.path("category").asText());
            scope = ContentExternalEvidence.Scope.valueOf(fact.path("scope").asText());
        } catch (IllegalArgumentException exception) {
            return null;
        }
        if ("movie".equals(input.type()) != (scope == ContentExternalEvidence.Scope.MOVIE)) return null;
        String text = clean(fact.path("text").asText(null));
        String sourceKey = urlKey(fact.path("sourceUrl").asText(null));
        Source source = searchedUrls.get(sourceKey);
        if (text == null || text.length() < 2 || text.length() > 240 || source == null
            || unsafeText(text)) return null;
        return new ContentExternalEvidence(category, text, scope, source.title(), source.url());
    }

    private void addSource(Map<String, Source> sources, String url, String title) {
        String key = urlKey(url);
        if (key == null) return;
        String safeTitle = clean(title);
        if (safeTitle == null || safeTitle.length() > 160 || unsafeText(safeTitle)) {
            safeTitle = URI.create(url.strip()).getHost();
        }
        sources.putIfAbsent(key, new Source(url.strip(), safeTitle));
    }

    private static String urlKey(String value) {
        if (value == null || value.length() > 2_000) return null;
        try {
            URI uri = URI.create(value.strip()).normalize();
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getUserInfo() != null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) return null;
            host = host.toLowerCase(Locale.ROOT);
            for (String blocked : BLOCKED_DOMAINS) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) return null;
            }
            String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
            String query = uri.getRawQuery() == null ? "" : Arrays.stream(uri.getRawQuery().split("&"))
                .filter(part -> !part.isBlank())
                .filter(part -> !part.split("=", 2)[0].toLowerCase(Locale.ROOT).matches(
                    "utm_.*|trackid|fbclid|gclid"))
                .sorted()
                .reduce((left, right) -> left + "&" + right)
                .map(filteredQuery -> "?" + filteredQuery).orElse("");
            return host + path + query;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String clean(String value) {
        if (value == null) return null;
        String text = value.strip().replaceAll("\\s+", " ");
        return text.isEmpty() ? null : text;
    }

    private static boolean unsafeText(String value) {
        return value.matches("(?s).*[<>\\p{Cc}\\p{Cf}].*")
            || UNSAFE_TEXT.matcher(value).find();
    }

    private JsonNode readJson(String value) {
        try (JsonParser parser = mapper.createParser(value)) {
            JsonNode root = mapper.readTree(parser);
            if (root == null || parser.nextToken() != null) throw invalidResponse();
            return root;
        } catch (IOException exception) {
            throw invalidResponse();
        }
    }

    private static ContentTaggingException invalidResponse() {
        return new ContentTaggingException("RESEARCH_INVALID_RESPONSE", true, false);
    }

    private record Source(String url, String title) { }
}
